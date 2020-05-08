package com.joshtalks.joshskills.core

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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.sentry.core.Sentry

const val HELP_ACTIVITY_REQUEST_CODE = 9010

abstract class BaseActivity : AppCompatActivity() {

    protected val TAG: String = javaClass.simpleName
    private lateinit var referrerClient: InstallReferrerClient

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
        InstallReferralUtil.installReferrer(applicationContext)
        Branch.getInstance().setIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
    }

    fun openHelpActivity() {
        val i = Intent(this, HelpActivity::class.java)
        startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
        AppAnalytics.create(AnalyticsEvent.HELP_SELECTED.NAME).push()
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
        } catch (ex: Exception) {
        }
    }

    private fun initUserForCrashlytics() {
        try {
            Sentry.captureMessage("Init Crashlytics")
            Crashlytics.getInstance().core.setUserName(User.getInstance().firstName)
            Crashlytics.getInstance().core.setUserEmail(User.getInstance().email)
            Crashlytics.getInstance()
                .core.setUserIdentifier(
                    User.getInstance().phoneNumber + "$" + Mentor.getInstance().getId()
                )

        } catch (ex: Exception) {
            Sentry.captureException(ex)
        }
    }

    private fun setupSentryUser() {
        try {
            Sentry.captureMessage("Init Sentry")
            val user = io.sentry.core.protocol.User()
            user.id = User.getInstance().phoneNumber
            user.username = User.getInstance().username
            Sentry.setUser(user)
        } catch (ex: Exception) {
            Sentry.captureException(ex)
        }
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
                .observe(this, Observer { workInfo ->
                    if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {

                    }
                })
        }
    }


}