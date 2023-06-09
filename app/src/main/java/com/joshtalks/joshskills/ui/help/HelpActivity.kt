package com.joshtalks.joshskills.ui.help

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.commit
import com.freshchat.consumer.sdk.Freshchat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.FRESH_CHAT_UNREAD_MESSAGES
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CategorySelectEventBus
import com.joshtalks.joshskills.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.joshtalks.joshskills.repository.server.help.Action
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class HelpActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var appAnalytics: AppAnalytics

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
        AppObjectController.getUnreadFreshchatMessages()
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
                            it.option.actionData?.run {
                                MixPanelTracker.publishEvent(MixPanelEvent.CALL_HELPLINE).push()
                                appAnalytics.addParam(
                                    AnalyticsEvent.CALL_HELPLINE.NAME,
                                    it.option.actionData.toString()
                                )
                                AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME)
                                    .addBasicParam()
                                    .addUserDetails()
                                    .push()
                                Utils.call(this@HelpActivity, this)
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
                            appAnalytics.addParam(
                                AnalyticsEvent.FAQ_SLECTED.NAME,
                                it.option.action.toString()
                            )
                            MixPanelTracker.publishEvent(MixPanelEvent.FAQ).push()
                            AppAnalytics.create(AnalyticsEvent.FAQ_SLECTED.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .push()
                            openFaqCategory()
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
                    showFaqFragment(it.selectedCategory, it.categoryList)
                })

        compositeDisposable.add(
            RxBus2.listen(FAQ::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    showFaqDetailsFragment(it)
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
}
