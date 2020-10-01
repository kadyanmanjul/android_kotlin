package com.joshtalks.joshskills.core

import android.app.Activity
import android.app.LauncherActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.android.installreferrer.api.InstallReferrerClient
import com.flurry.android.FlurryAgent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener
import com.google.firebase.inappmessaging.ktx.inAppMessaging
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.ui.extra.SignUpPermissionDialogFragment
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.inbox.IS_FROM_NEW_ONBOARDING
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.nps.NetPromoterScoreFragment
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.OnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.newrelic.agent.android.NewRelic
import com.uxcam.OnVerificationListener
import com.uxcam.UXCam
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.lang.reflect.Type
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import timber.log.Timber

const val HELP_ACTIVITY_REQUEST_CODE = 9010

abstract class BaseActivity : AppCompatActivity(), LifecycleObserver,
    FirebaseInAppMessagingImpressionListener, FirebaseInAppMessagingClickListener {

    private lateinit var referrerClient: InstallReferrerClient
    private val versionResponseTypeToken: Type = object : TypeToken<VersionResponse>() {}.type
    private var versionResponse: VersionResponse? = null

    enum class ActivityEnum {
        Conversation,
        CourseProgress,
        CourseExplore,
        Help,
        Inbox,
        Launcher,
        CourseDetails,
        Onboard,
        Signup,
        Empty,
        DeepLink
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.status_bar_color)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
        JoshSkillExecutors.BOUNDED.submit {
            initUserForCrashlytics()
            initIdentifierForTools()
            InstallReferralUtil.installReferrer(applicationContext)
        }
        if (AppObjectController.getFirebaseRemoteConfig()
                .getBoolean(FirebaseRemoteConfigKey.UX_CAM_FEATURE_ENABLE)
        ) {
            UXCam.startWithKey(BuildConfig.WX_CAM_KEY)
        }
    }

    private fun initIdentifierForTools() {
        if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
            Branch.getInstance().setIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
            initNewRelic()
            initFlurry()
        }
        UXCam.setUserIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
        // UXCam.setUserProperty(String propertyName , String value)

        UXCam.addVerificationListener(object : OnVerificationListener {
            override fun onVerificationSuccess() {
                FirebaseCrashlytics.getInstance()
                    .setCustomKey("UXCam_Recording_Link", UXCam.urlForCurrentSession())
            }

            override fun onVerificationFailed(errorMessage: String) {
                Timber.e(errorMessage)
            }
        })
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
            is CourseDetailsActivity -> ActivityEnum.CourseDetails
            is OnBoardActivity -> ActivityEnum.Onboard
            is SignUpActivity -> ActivityEnum.Signup
            else -> ActivityEnum.Empty
        }
    }

    fun getIntentForState(): Intent? {
        val intent: Intent? = when {
            User.getInstance().isVerified.not() -> {
                if (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false)) {
                    getInboxActivityIntent()
                } else {
                    Intent(this, OnBoardActivity::class.java)
                }
            }
            isUserProfileComplete() -> {
                Intent(this, SignUpActivity::class.java)
            }
            else -> getInboxActivityIntent()
        }
        return intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getInboxActivityIntent(isFromOnBoardingFlow: Boolean = false): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SHOW_OVERLAY, true)
            putExtra(IS_FROM_NEW_ONBOARDING, isFromOnBoardingFlow)
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
            if (mIntent != null && mIntent.hasExtra(HAS_NOTIFICATION) && mIntent.hasExtra(
                    NOTIFICATION_ID
                ) && mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not()
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
            val user = User.getInstance()
            val mentor = Mentor.getInstance()
            val gaid = PrefManager.getStringValue(USER_UNIQUE_ID, false)

            val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
            firebaseCrashlytics.setUserId(gaid)
            firebaseCrashlytics.setCustomKey("gaid", gaid)
            firebaseCrashlytics.setCustomKey("mentor_id", mentor.getId())
            firebaseCrashlytics.setCustomKey("phone", getPhoneNumber())
            firebaseCrashlytics.setCustomKey("first_name", user.firstName)
            firebaseCrashlytics.setCustomKey("email", user.email)
            firebaseCrashlytics.setCustomKey("username", user.username)
            firebaseCrashlytics.setCustomKey("user_type", user.userType)
            firebaseCrashlytics.setCustomKey(
                "age",
                AppAnalytics.getAge(user.dateOfBirth).toString() + ""
            )
            user.dateOfBirth?.let { firebaseCrashlytics.setCustomKey("date_of_birth", it) }
            firebaseCrashlytics.setCustomKey(
                "gender",
                if (user.gender == "M") "MALE" else "FEMALE"
            )
            FirebaseCrashlytics.getInstance().setUserId(
                PrefManager.getStringValue(
                    USER_UNIQUE_ID
                )
            )
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
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

    protected fun feedbackEngagementStatus(question: Question?) {
        if (question != null && question.needFeedback == null) {
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(WorkManagerAdmin.getQuestionFeedback(question.questionId))
                .observe(this, Observer {
                })
        }
    }

    protected fun showNetPromoterScoreDialog(nps: NPSEvent? = null) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            canShowNPSDialog(nps)
        }
    }

    protected suspend fun canShowNPSDialog(nps: NPSEvent? = null, id: String = EMPTY): Boolean {
        return CoroutineScope(Dispatchers.IO).async(Dispatchers.IO) {
            val currentState: NPSEvent? = getCurrentNpsState(nps) ?: return@async false
            val minNpsInADay = AppObjectController.getFirebaseRemoteConfig()
                .getDouble("MINIMUM_NPS_IN_A_DAY_COUNT").toInt()
            val totalCountToday =
                AppObjectController.appDatabase.npsEventModelDao().getTotalCountOfRows()
            if (totalCountToday >= minNpsInADay) {
                return@async false
            }
            val npsEventModel =
                NPSEventModel.getAllNpaList()?.filter { it.enable }
                    ?.find { it.event == currentState }
                    ?: return@async false
            npsEventModel.eventId = id
            getQuestionForNPS(npsEventModel)
            return@async true
        }.await()
    }

    private fun getCurrentNpsState(nps: NPSEvent? = null): NPSEvent? {
        var currentState = nps
        if (currentState == null) {
            currentState = NPSEventModel.getCurrentNPA()
        }
        return currentState
    }

    private fun getQuestionForNPS(
        eventModel: NPSEventModel
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val observer = Observer<WorkInfo> { workInfo ->
                try {
                    val currentState: NPSEvent? = getCurrentNpsState()
                    if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED && currentState != null) {
                        val output = workInfo.outputData.getString("nps_question_list")
                        if (output.isNullOrEmpty().not()) {
                            NPSEventModel.removeCurrentNPA()
                            val questionList =
                                NPSQuestionModel.getNPSQuestionModelList(output!!)
                            if (questionList.isNullOrEmpty().not()) {
                                showNPSFragment(eventModel, questionList!!)
                                CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                                    AppObjectController.appDatabase.npsEventModelDao()
                                        .insertNPSEvent(eventModel)
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

                WorkManager.getInstance(applicationContext)
                    .getWorkInfoByIdLiveData(WorkManagerAdmin.getQuestionNPA(eventModel.eventName))
                    .removeObservers(this@BaseActivity)
            }
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(WorkManagerAdmin.getQuestionNPA(eventModel.eventName))
                .observe(this@BaseActivity, observer)
        }
    }

    private fun showNPSFragment(
        npsModel: NPSEventModel,
        questionList: List<NPSQuestionModel>
    ) {
        val bottomSheetFragment = NetPromoterScoreFragment.newInstance(npsModel, questionList)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    fun showSignUpDialog() {
        SignUpPermissionDialogFragment.showDialog(supportFragmentManager)
    }

    fun checkForOemNotifications() {
        val oemIntent = PowerManagers.getIntentForOEM(this)
        if (oemIntent != null && shouldRequireCustomPermission()) {
            CustomPermissionDialogFragment.showCustomPermissionDialog(
                oemIntent,
                supportFragmentManager
            )
        }
    }

    fun shouldRequireCustomPermission(): Boolean {
        val oemIntent = PowerManagers.getIntentForOEM(this)
        val performedAction = PrefManager.getStringValue(CUSTOM_PERMISSION_ACTION_KEY)
        return oemIntent != null && performedAction == EMPTY
    }

    fun isUserProfileComplete(): Boolean {
        try {
            val user = User.getInstance()
            if (user.phoneNumber.isNotEmpty() && user.firstName.isEmpty()) {
                return true
            }
            if (user.firstName.isEmpty()) {
                return true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

    fun replaceFragment(
        containerId: Int,
        fragment: Fragment,
        newFragmentTag: String,
        currentFragmentTag: String? = null
    ) {
        if (currentFragmentTag == null) {
            supportFragmentManager.beginTransaction().replace(containerId, fragment, newFragmentTag)
                .commit()
        } else {
            supportFragmentManager.beginTransaction().replace(containerId, fragment, newFragmentTag)
                .addToBackStack(currentFragmentTag).commit()
        }
    }

    fun isGuestUser(): Boolean {
        if (User.getInstance().isVerified) {
            return !PrefManager.getBoolValue(IS_GUEST_ENROLLED)
        } else {
            return true
        }
    }

    fun logout() {
        AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
            .addUserDetails()
            .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, true).push()
        val intent =
            Intent(
                AppObjectController.joshApplication,
                SignUpActivity::class.java
            )
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FLOW_FROM, "CourseExploreActivity")
        }
        CoroutineScope(Dispatchers.IO).launch {
            PrefManager.clearUser()
            AppObjectController.joshApplication.startActivity(intent)
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun addInAppMessagingListener() {
        FirebaseInAppMessaging.getInstance().isAutomaticDataCollectionEnabled = true
        Firebase.inAppMessaging.addImpressionListener(this)
        Firebase.inAppMessaging.addClickListener(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun removeInAppMessagingListener() {
        FirebaseInAppMessaging.getInstance().removeImpressionListener(this)
        FirebaseInAppMessaging.getInstance().removeClickListener(this)
    }

    override fun impressionDetected(p0: InAppMessage) {
    }

    override fun messageClicked(inAppMessage: InAppMessage, action: Action) {
        JoshSkillExecutors.BOUNDED.execute {
            Uri.parse(action.actionUrl).host?.trim()?.toLowerCase(Locale.getDefault()).run {
                when {
                    this == getString(R.string.conversation_open_dlink) -> {
                        val courseId = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        AppObjectController.appDatabase.courseDao().getCourseAccordingId(courseId)
                            ?.let {
                                ConversationActivity.startConversionActivity(this@BaseActivity, it)
                            }
                    }
                    this == getString(R.string.setting_dlink) -> {

                    }
                    this == getString(R.string.referral_open_dlink) -> {
                        ReferralActivity.startReferralActivity(this@BaseActivity)
                    }
                    this == getString(R.string.reminder_open_dlink) -> {

                    }
                    this == getString(R.string.video_open_dlink) -> {

                    }
                    this == getString(R.string.assessment_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            AssessmentActivity.startAssessmentActivity(
                                this@BaseActivity,
                                id.toInt()
                            )
                        }
                    }
                    this == getString(R.string.landing_page_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            CourseDetailsActivity.startCourseDetailsActivity(
                                this@BaseActivity, id.toInt(),
                                flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            )
                        }
                    }
                    this == getString(R.string.payment_summary_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            PaymentSummaryActivity.startPaymentSummaryActivity(
                                this@BaseActivity, id
                            )
                        }
                    }
                    this == getString(R.string.calling_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                                this@BaseActivity, id
                            )
                        }
                    }
                    this == getString(R.string.url_dlink) -> {
                        val url = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (url.isNotEmpty()) {
                            Utils.openUrl(url, this@BaseActivity)
                        }
                    }
                    this == getString(R.string.course_explore_dlink) -> {
                        CourseExploreActivity.startCourseExploreActivity(
                            this@BaseActivity,
                            COURSE_EXPLORER_CODE,
                            null,
                            state = ActivityEnum.DeepLink
                        )
                    }
                    else -> {
                        return@execute
                    }
                }
            }
        }

    }

}
