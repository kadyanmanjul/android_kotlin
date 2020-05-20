package com.joshtalks.joshskills.ui.help

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.commit
import com.freshchat.consumer.sdk.Freshchat
import com.freshchat.consumer.sdk.FreshchatConfig
import com.freshchat.consumer.sdk.FreshchatNotificationConfig
import com.freshchat.consumer.sdk.j.af
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CategorySelectEventBus
import com.joshtalks.joshskills.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.User.Companion.getInstance
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import com.joshtalks.joshskills.repository.server.help.Action
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HelpActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var appAnalytics: AppAnalytics
    private lateinit var freshChat: Freshchat

    var restoreIdReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val restoreId = freshChat.user.restoreId
                        if (restoreId.isBlank().not()) {
                            PrefManager.put(RESTORE_ID, restoreId)
                            val requestMap = mutableMapOf<String, String?>()
                            requestMap["restore_id"] = restoreId
                            AppObjectController.commonNetworkService.postFreshChatRestoreIDAsync(
                                PrefManager.getStringValue(USER_UNIQUE_ID),
                                requestMap
                            )
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setToolbar()
        openListOfHelp()
        appAnalytics = AppAnalytics.create(AnalyticsEvent.HELP_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
        initialiseFreshChat()
        AppObjectController.getLocalBroadcastManager()
            .registerReceiver(restoreIdReceiver, IntentFilter(Freshchat.FRESHCHAT_USER_RESTORE_ID_GENERATED))
    }

    fun initialiseFreshChat() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                freshChat = Freshchat.getInstance(AppObjectController.joshApplication)
                val config = FreshchatConfig(
                    BuildConfig.FRESH_CHAT_APP_ID,
                    BuildConfig.FRESH_CHAT_APP_KEY
                )
                af.eK()?.let { Freshchat.setImageLoader(it) }
                config.isCameraCaptureEnabled = true
                config.isGallerySelectionEnabled = true
                config.isResponseExpectationEnabled = true
                config.domain = "https://msdk.in.freshchat.com"
                freshChat.init(config)
                var restoreId: String? = null
                try {
                    restoreId = if (PrefManager.getStringValue(RESTORE_ID).isBlank()) {
                        val id = PrefManager.getStringValue(USER_UNIQUE_ID)
                        val details =
                            AppObjectController.commonNetworkService.getFreshChatRestoreIdAsync(id)
                        if (details.restoreId.isNullOrBlank().not()) {
                            details.restoreId
                        } else null
                    } else PrefManager.getStringValue(RESTORE_ID)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                updateFreshchatSdkUserProperties()
                freshChat.identifyUser(PrefManager.getStringValue(USER_UNIQUE_ID), restoreId)
                val notificationConfig = FreshchatNotificationConfig()
                    .setImportance(NotificationManagerCompat.IMPORTANCE_MAX)
                freshChat.setNotificationConfig(notificationConfig)
                freshChat.setPushRegistrationToken(PrefManager.getStringValue(FCM_TOKEN))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateFreshchatSdkUserProperties() {
        val freshchatUser = freshChat.user
        freshchatUser.firstName = getInstance().firstName
        freshchatUser.email = getInstance().email
        val mobileNumber = getPhoneNumber()
        if (mobileNumber.isNotEmpty()) {
            val length = mobileNumber.length
            if (length > 10) {
                freshchatUser.setPhone(mobileNumber.substring(0, length - 10), mobileNumber.substring(length - 10))
            }
        } else freshchatUser.setPhone("+91", "XXXXXXXXXX")
        freshChat.user = freshchatUser

        val userMeta: MutableMap<String, String?> = HashMap()
        userMeta["Username"] = User.getInstance().firstName
        userMeta["Email_id"] = User.getInstance().email
        userMeta["Mobile_no"] = getPhoneNumber()
        userMeta["Age"] =
            AppAnalytics.getAge(User.getInstance().dateOfBirth).toString()
        userMeta["Gender"] = User.getInstance().gender
        if (Mentor.getInstance().hasId()) {
            userMeta["Mentor_id"] = Mentor.getInstance().getId()
            userMeta["Login_type"] = "yes"
            userMeta["Subscribed_user"] = "yes"
            try {
                val allConversationId = appDatabase.courseDao().getAllConversationId()
                userMeta["courses_availed"] = allConversationId.size.toString()
                for (i in allConversationId.indices) {
                    userMeta["courses_$i"] = appDatabase.courseDao().chooseRegisterCourseMinimal(
                        allConversationId[i]
                    )?.course_name
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        //Call setUserProperties to sync the user properties with Freshchat's servers
        try {
            freshChat.setUserProperties(userMeta)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private fun setToolbar() {
        findViewById<View>(R.id.iv_help).visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }
    }


    private fun openListOfHelp() {
        supportFragmentManager.commit(true) {
            addToBackStack(HelpListFragment::class.java.name)
            add(
                R.id.container,
                HelpListFragment.newInstance(),
                HelpListFragment::class.java.name
            )
        }
    }

    private fun openFaqCategory() {
        supportFragmentManager.commit(true) {
            addToBackStack(FaqCategoryFragment::class.java.name)
            replace(
                R.id.container,
                FaqCategoryFragment.newInstance(),
                FaqCategoryFragment::class.java.name
            )
        }
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (supportFragmentManager.backStackEntryCount == 1) {
            appAnalytics.addParam(AnalyticsEvent.HELP_BACK_CLICKED.NAME, true)
            this@HelpActivity.finish()
            return
        }
        super.onBackPressed()
    }


    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
        freshChat.getUnreadCountAsync { _, unreadCount ->
            PrefManager.put(FRESH_CHAT_UNREAD_MESSAGES, unreadCount)
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(HelpRequestEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when {
                        Action.CALL == it.option.action -> {
                            val number = if (PrefManager.getBoolValue(IS_COURSE_BOUGHT) || PrefManager.getBoolValue(
                                    IS_SUBSCRIPTION_STARTED)) {
                                it.option.actionData
                            } else {
                                it.option.actionDataForFreeTrial
                            }
                            if (number != null) {
                                appAnalytics.addParam(AnalyticsEvent.CALL_HELPLINE.NAME, number.toString())
                                MixPanelTracker.publishEvent(MixPanelEvent.CALL_HELPLINE).push()
                                AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME)
                                    .addBasicParam()
                                    .addUserDetails()
                                    .push()
                                Utils.call(this@HelpActivity, number)
                            }
                        }
                        Action.HELPCHAT == it.option.action -> {
                            appAnalytics.addParam(
                                AnalyticsEvent.HELP_CHAT.NAME,
                                it.option.action.toString()
                            )
                            MixPanelTracker.publishEvent(MixPanelEvent.CHAT_WITH_AGENT).push()
                            AppAnalytics.create(AnalyticsEvent.HELP_CHAT.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .push()
                            Freshchat.showConversations(applicationContext)
                            PrefManager.put(FRESH_CHAT_UNREAD_MESSAGES, 0)
                        }
                        Action.FAQ == it.option.action -> {
//                            appAnalytics.addParam(
//                                AnalyticsEvent.FAQ_SLECTED.NAME,
//                                it.option.action.toString()
//                            )
//                            MixPanelTracker.publishEvent(MixPanelEvent.FAQ).push()
//                            AppAnalytics.create(AnalyticsEvent.FAQ_SLECTED.NAME)
//                                .addBasicParam()
//                                .addUserDetails()
//                                .push()
//                            openFaqCategory()
                            compliantScreen()
                        }
                        else -> {
                            showToast(getString(R.string.something_went_wrong))
                        }
                    }
                })


        compositeDisposable.add(
            RxBus2.listen(CategorySelectEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    compliantScreen()
                   // showFaqFragment(it.selectedCategory, it.categoryList)
                })

        compositeDisposable.add(
            RxBus2.listen(FAQ::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    compliantScreen()
                    //showFaqDetailsFragment(it)
                })
    }

    private fun showFaqFragment(selectedCategory: FAQCategory, categoryList: List<FAQCategory>) {
        appAnalytics.addParam(
            AnalyticsEvent.FAQ_CATEGORY_SELECTED.NAME,
            selectedCategory.categoryName
        )
        supportFragmentManager.commit(true) {
            addToBackStack(FaqFragment::class.java.name)
            replace(
                R.id.container,
                FaqFragment.newInstance(selectedCategory, ArrayList(categoryList)),
                FaqFragment::class.java.name
            )
        }
    }

    private fun showFaqDetailsFragment(faq: FAQ) {
        appAnalytics.addParam(AnalyticsEvent.FAQ_SELECTED.NAME, faq.id)
        supportFragmentManager.commit(true) {
            addToBackStack(FaqDetailsFragment::class.java.name)
            add(
                R.id.container,
                FaqDetailsFragment.newInstance(faq),
                FaqDetailsFragment::class.java.name
            )
        }
    }

    override fun onDestroy() {
        AppObjectController.getLocalBroadcastManager().unregisterReceiver(restoreIdReceiver)
        super.onDestroy()
    }

    private fun compliantScreen() {
        supportFragmentManager.commit(true) {
            addToBackStack(ComplaintFragment::class.java.name)
            replace(
                R.id.container,
                ComplaintFragment.newInstance(),
                ComplaintFragment::class.java.name
            )
        }
    }
}
