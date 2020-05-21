package com.joshtalks.joshskills.core

import android.app.Activity
import android.app.LauncherActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.android.installreferrer.api.InstallReferrerClient
import com.crashlytics.android.Crashlytics
import com.flurry.android.FlurryAgent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.nps.NPAFilter
import com.joshtalks.joshskills.repository.local.model.nps.NPSEvent
import com.joshtalks.joshskills.repository.local.model.nps.NPSEventModel
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.nps.NetPromoterScoreFragment
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.newrelic.agent.android.NewRelic
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.sentry.core.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val HELP_ACTIVITY_REQUEST_CODE = 9010

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var referrerClient: InstallReferrerClient

    enum class ActivityEnum {
        Conversation,
        CourseProgress,
        CourseExplore,
        Help,
        Inbox,
        Launcher,
        Payment,
        Onboard,
        Signup,
        Empty
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor =
                ContextCompat.getColor(applicationContext, R.color.status_bar_color)
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
        initUserForCrashlytics()
        initIdentifierForTools()
        InstallReferralUtil.installReferrer(applicationContext)
    }

    private fun initIdentifierForTools() {
        if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
            Branch.getInstance().setIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
            setupSentryUser()
            initNewRelic()
            initFlurry()
        }
    }

    fun openHelpActivity() {
        val i = Intent(this, HelpActivity::class.java)
        startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
    }

    fun getActivityType(act: Activity): ActivityEnum {
        return when (act) {
            is ConversationActivity -> ActivityEnum.Conversation
            is CourseExploreActivity -> ActivityEnum.CourseExplore
            is CourseProgressActivity -> ActivityEnum.CourseProgress
            is HelpActivity -> ActivityEnum.Help
            is InboxActivity -> ActivityEnum.Inbox
            is LauncherActivity -> ActivityEnum.Launcher
            is PaymentActivity -> ActivityEnum.Payment
            is OnBoardActivity -> ActivityEnum.Onboard
            is SignUpActivity -> ActivityEnum.Signup
            else -> ActivityEnum.Empty
        }
    }

    fun getIntentForState(): Intent? {
        val intent: Intent? = if (User.getInstance().token == null) {
            Intent(this, OnBoardActivity::class.java)
        } else {
            getInboxActivityIntent()
        }
        return intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getCroppingActivity(filePath: String): Intent {
        return Intent(this, CropImageActivity::class.java).apply {
            putExtra(SOURCE_IMAGE, filePath)
        }
    }

    fun getInboxActivityIntent(): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    protected fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    protected fun processIntent(mIntent: Intent?) {
        try {
            if (!(mIntent == null || !mIntent.hasExtra(HAS_NOTIFICATION) || !mIntent.hasExtra(
                    NOTIFICATION_ID
                ) || !mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not())
            ) {
                EngagementNetworkHelper.clickNotification(
                    mIntent.getStringExtra(
                        NOTIFICATION_ID
                    )
                )
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    private fun initUserForCrashlytics() {
        try {
            Crashlytics.getInstance().core.setUserName(User.getInstance().firstName)
            Crashlytics.getInstance().core.setUserEmail(User.getInstance().email)
            Crashlytics.getInstance().core.setUserIdentifier(
                PrefManager.getStringValue(
                    USER_UNIQUE_ID
                )
            )
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    private fun setupSentryUser() {
        val user = io.sentry.core.protocol.User()
        user.id = PrefManager.getStringValue(USER_UNIQUE_ID)
        user.username = User.getInstance().username
        Sentry.setUser(user)

    }

    private fun initNewRelic() {
        NewRelic.setUserId(PrefManager.getStringValue(USER_UNIQUE_ID))
    }

    private fun initFlurry() {
        FlurryAgent.setUserId(PrefManager.getStringValue(USER_UNIQUE_ID))
    }

    fun callHelpLine() {
        AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME).push()
        Utils.call(this, AppObjectController.getFirebaseRemoteConfig().getString("helpline_number"))
    }


    protected fun isUserHaveNotPersonalDetails(): Boolean {
        return User.getInstance().dateOfBirth.isNullOrEmpty()
    }

    protected fun getPersonalDetailsActivityIntent(): Intent {
        return Intent(this, ProfileActivity::class.java)
    }

    protected fun feedbackEngagementStatus(question: Question?) {
        if (question != null && question.needFeedback == null) {
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(WorkMangerAdmin.getQuestionFeedback(question.questionId))
                .observe(this, Observer {
                    /* if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {

                     }*/
                })
        }
    }


    protected fun showNPSDialog(nps: NPSEvent? = null, courseId: String = EMPTY): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            var currentState = nps
            if (currentState == null) {
                currentState = NPSEventModel.getCurrentNPA()
            }

            if (currentState == null) {
                return@launch
            }

            val list =
                NPSEventModel.getAllNpaList()?.filter { it.enable }
            val npsEventModel = list?.find { it.event == currentState }
            npsEventModel?.let { npsEventModelInternal ->
                if (npsEventModelInternal.filterBy == NPAFilter.DAY) {
                    val date =
                        AppObjectController.appDatabase.courseDao().getCourseCreatedDate(courseId)
                    val (flag, _) = Utils.compareDateToday(date, npsEventModelInternal.day)
                    if (flag.not()) {
                        return@launch
                    }
                }

                CoroutineScope(Dispatchers.Main).launch {
                    WorkManager.getInstance(applicationContext)
                        .getWorkInfoByIdLiveData(
                            WorkMangerAdmin.getQuestionNPA(
                                npsEventModelInternal.eventName
                            )
                        )
                        .observe(this@BaseActivity, Observer {
                            try {
                                if (it != null && it.state == WorkInfo.State.SUCCEEDED) {
                                    val output = it.outputData.getString("nps_question_list")
                                    if (output.isNullOrEmpty().not()) {
                                        NPSEventModel.setCurrentNPA(null)
                                        val questionList =
                                            NPSQuestionModel.getNPSQuestionModelList(output!!)
                                        if (questionList.isNullOrEmpty().not()) {
                                            showNPSFragment(questionList!!)
                                        }
                                    }
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        })
                }
            }
        }
        return true
    }

    private fun showNPSFragment(questionList: List<NPSQuestionModel>) {
        val bottomSheetFragment = NetPromoterScoreFragment.newInstance(questionList)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

    }

}