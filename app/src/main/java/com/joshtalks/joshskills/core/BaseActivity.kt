package com.joshtalks.joshskills.core

//import com.uxcam.OnVerificationListener
//import com.uxcam.UXCam
import android.annotation.SuppressLint
import android.app.Activity
import android.app.LauncherActivity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.android.installreferrer.api.InstallReferrerClient
import com.google.android.gms.location.LocationRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener
import com.google.firebase.inappmessaging.ktx.inAppMessaging
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.track.TrackActivity
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.ui.extra.SignUpPermissionDialogFragment
import com.joshtalks.joshskills.ui.gif.GIFActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.ui.nps.NetPromoterScoreFragment
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.points_history.PointsHistoryActivity
import com.joshtalks.joshskills.ui.points_history.SpokenHistoryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.OnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.termsandconditions.WebViewFragment
import com.joshtalks.joshskills.ui.userprofile.ShowAnimatedLeaderBoardFragment
import com.joshtalks.joshskills.ui.userprofile.ShowAwardFragment
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import com.patloew.colocation.CoLocation
import io.branch.referral.Branch
import io.branch.referral.Defines
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.sentry.Sentry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.util.*

const val HELP_ACTIVITY_REQUEST_CODE = 9010
const val COURSE_EXPLORER_NEW = 2008
const val REQUEST_SHOW_SETTINGS = 123

abstract class BaseActivity :
        TrackActivity(),
        LifecycleObserver,
        FirebaseInAppMessagingImpressionListener,
        FirebaseInAppMessagingClickListener {

    private lateinit var referrerClient: InstallReferrerClient
    private val versionResponseTypeToken: Type = object : TypeToken<VersionResponse>() {}.type
    private var versionResponse: VersionResponse? = null
    var videoChatObject: ChatModel? = null

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
        DeepLink,
        FreeTrial
    }

    var openSettingActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (AppObjectController.isSettingUpdate) {
            reCreateActivity()
        }
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
        lifecycleScope.launch(Dispatchers.IO) {
            initUserForCrashlytics()
            initIdentifierForTools()
            InstallReferralUtil.installReferrer(applicationContext)
            try {
                processBranchDynamicLinks()
                processFirebaseDynamicLinks()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            //addScreenRecording()
        }
    }

    /*private fun addScreenRecording() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (BuildConfig.DEBUG.not()) {
                if (AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.UX_CAM_FEATURE_ENABLE)
                ) {
                    UXCam.startWithKey(BuildConfig.WX_CAM_KEY)
                }
                if (AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.SMART_LOOK_FEATURE_ENABLE)
                ) {
                    Smartlook.registerIntegrationListener(object : IntegrationListener {
                        override fun onSessionReady(dashboardSessionUrl: String) {
                            Timber.tag("baseactivity").e(dashboardSessionUrl)
                            FirebaseCrashlytics.getInstance()
                                .setCustomKey("Smartlook_session_Link", dashboardSessionUrl)
                        }

                        override fun onVisitorReady(dashboardVisitorUrl: String) {
                            Timber.tag("baseactivity1").e(dashboardVisitorUrl)
                            FirebaseCrashlytics.getInstance()
                                .setCustomKey("Smartlook_visitor_Link", dashboardVisitorUrl)
                        }
                    })
                    val id = if (Mentor.getInstance().hasId()) {
                        Mentor.getInstance().getId()
                    } else {
                        PrefManager.getStringValue(USER_UNIQUE_ID)
                    }
                    Smartlook.setUserIdentifier(id)
                    Smartlook.setUserProperties(UserProperties())
                    if (Smartlook.isRecording().not()) {
                        Smartlook.startRecording()
                    }
                    Smartlook.enableIntegration(FirebaseCrashlyticsIntegration())
                }
            }
        }
    }*/

    private fun processFirebaseDynamicLinks() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener {
                    try {
                        val referralCode = it?.utmParameters?.getString("utm_source", EMPTY)
                                ?: EMPTY
                        //it.
                        val installReferrerModel =
                                InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()
                        if (referralCode != EMPTY) {
                            installReferrerModel.utmSource = referralCode
                        }
                        InstallReferrerModel.update(installReferrerModel)
                        Timber.d("DeepLink : $referralCode")
                        Timber.d("installReferrerModel : $installReferrerModel")
                    } catch (ex: java.lang.Exception) {
                        Timber.e(ex)
                    }
                }
                .addOnFailureListener {
                    Timber.w("getDynamicLink:onFailure : $it")
                }
    }

    private fun processBranchDynamicLinks() {
        Branch.sessionBuilder(WeakReference(this@BaseActivity).get())
                .withCallback { referringParams, error ->
                    try {
                        val jsonParams =
                                referringParams ?: (Branch.getInstance().firstReferringParams
                                        ?: Branch.getInstance().latestReferringParams)
                        val installReferrerModel =
                                InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()
                        jsonParams?.let {
                            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                            if (it.has(Defines.Jsonkey.ReferralCode.key))
                                installReferrerModel.utmSource = it.getString(Defines.Jsonkey.ReferralCode.key)
                            if (it.has(Defines.Jsonkey.UTMMedium.key))
                                installReferrerModel.utmMedium = it.getString(Defines.Jsonkey.UTMMedium.key)
                            if (it.has(Defines.Jsonkey.UTMCampaign.key))
                                installReferrerModel.utmTerm = it.getString(Defines.Jsonkey.UTMCampaign.key)
                        }
                        if (isFinishing.not()) {
                            Log.i(TAG, "processBranchDynamicLinks: $installReferrerModel")
                            InstallReferrerModel.update(installReferrerModel)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                        LogException.catchException(ex)
                    }
                }.withData(this@BaseActivity.intent.data).init()
    }


    private fun initIdentifierForTools() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                try {
                    Branch.getInstance().setIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                initNewRelic()
                setupSentryUser()
                //UXCam.setUserIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
                // UXCam.setUserProperty(String propertyName , String value)

                /*UXCam.addVerificationListener(object : OnVerificationListener {
                    override fun onVerificationSuccess() {
                        FirebaseCrashlytics.getInstance()
                            .setCustomKey("UXCam_Recording_Link", UXCam.urlForCurrentSession())
                    }

                    override fun onVerificationFailed(errorMessage: String) {
                        Timber.e(errorMessage)
                    }
                })*/
            }
            /*UXCam.setUserIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
            // UXCam.setUserProperty(String propertyName , String value)

            UXCam.addVerificationListener(object : OnVerificationListener {
                override fun onVerificationSuccess() {
                    FirebaseCrashlytics.getInstance()
                        .setCustomKey("UXCam_Recording_Link", UXCam.urlForCurrentSession())
                }

                override fun onVerificationFailed(errorMessage: String) {
                    Timber.e(errorMessage)
                }
            })*/
        }
    }

    fun openHelpActivity() {
        val i = Intent(this, HelpActivity::class.java)
        startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
    }

    fun openGifActivity(conversationId: String?) {
        startActivity(
                Intent(this, GIFActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, conversationId)
                }
        )
    }

    fun openLeaderBoard(conversationId: String, courseId: String?) {
        val i = Intent(this, LeaderBoardViewPagerActivity::class.java).apply {
            putExtra(CONVERSATION_ID, conversationId)
            putExtra(COURSE_ID, courseId)
        }
        startActivity(i)
    }

    fun openPointHistory(mentorId: String? = null, conversationId: String? = null) {
        PointsHistoryActivity.startPointHistory(this, mentorId, conversationId)
    }

    fun openSpokenMinutesHistory(mentorId: String? = null, conversationId: String? = null) {
        SpokenHistoryActivity.startSpokenMinutesHistory(this, mentorId, conversationId)
    }

    fun getActivityType(act: Activity): BaseActivity.ActivityEnum {
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

    fun getIntentForState(): Intent {
        val intent: Intent = when {
            User.getInstance().isVerified.not() -> {
                when {
                    (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false) &&
                            PrefManager.getBoolValue(IS_PAYMENT_DONE, false).not()) -> {
                        getInboxActivityIntent()
                    }
                    PrefManager.getBoolValue(IS_PAYMENT_DONE, false) -> {
                        Intent(this, SignUpActivity::class.java)
                    }
                    PrefManager.getBoolValue(IS_FREE_TRIAL, false, false) -> {
                        Intent(this, FreeTrialOnBoardActivity::class.java)
                    }
                    else -> {
                        Intent(this, OnBoardActivity::class.java)
                    }
                }
            }
            isUserProfileNotComplete() -> {
                Intent(this, SignUpActivity::class.java)
            }
            containsFavUserCallBackUrl() -> {
                getWebRtcActivityIntent()
            }
            else -> getInboxActivityIntent()
        }
        return intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun isUserProfileNotComplete(): Boolean {
        try {
            val user = User.getInstance()
            if (user.phoneNumber.isNullOrEmpty() && user.firstName.isNullOrEmpty()) {
                return true
            }
            if (user.firstName.isNullOrEmpty()) {
                return true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

    fun containsFavUserCallBackUrl(): Boolean {
        try {
            return intent?.dataString?.contains("app.joshtalks.org/sht/ag") ?: false
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

    fun getWebRtcActivityIntent(): Intent {
        val partnerUid = intent?.dataString?.split("/")?.lastOrNull()?.toInt()
        return if (partnerUid != null) {
            WebRtcActivity.getFavMissedCallbackIntent(partnerUid, this).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            getInboxActivityIntent()
        }
    }

    fun getInboxActivityIntent(isFromOnBoardingFlow: Boolean = false): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //putExtra(SHOW_OVERLAY, true)
            //putExtra(IS_FROM_NEW_ONBOARDING, isFromOnBoardingFlow)
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
            lifecycleScope.launch(Dispatchers.IO) {
                if (mIntent != null && mIntent.hasExtra(HAS_NOTIFICATION) &&
                        mIntent.hasExtra(NOTIFICATION_ID) &&
                        mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not()
                ) {
                    EngagementNetworkHelper.clickNotification(
                            mIntent.getStringExtra(
                                    NOTIFICATION_ID
                            )
                    )
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    private fun initUserForCrashlytics() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = User.getInstance()
                val mentor = Mentor.getInstance()
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID, false)

                val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
                firebaseCrashlytics.setUserId(gaid)
                firebaseCrashlytics.setCustomKey("gaid", gaid)
                firebaseCrashlytics.setCustomKey("mentor_id", mentor.getId())
                firebaseCrashlytics.setCustomKey("phone", getPhoneNumber())
                firebaseCrashlytics.setCustomKey("first_name", user.firstName ?: EMPTY)
                firebaseCrashlytics.setCustomKey("email", user.email ?: EMPTY)
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
    }

    private fun setupSentryUser() {
        try {
            val user = io.sentry.protocol.User()
            user.id = PrefManager.getStringValue(USER_UNIQUE_ID)
            user.username = User.getInstance().username
            user.email = User.getInstance().email
            Sentry.setUser(user)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun initNewRelic() {
        //   NewRelic.setUserId(PrefManager.getStringValue(USER_UNIQUE_ID))
    }

    fun callHelpLine() {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME).push()
            Utils.call(
                    this@BaseActivity,
                    AppObjectController.getFirebaseRemoteConfig().getString("helpline_number")
            )
        }
    }

    protected fun isUserHaveNotPersonalDetails(): Boolean {
        return User.getInstance().dateOfBirth.isNullOrEmpty()
    }

    protected fun showNetPromoterScoreDialog(nps: NPSEvent? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            canShowNPSDialog(nps)
        }
    }

    protected suspend fun canShowNPSDialog(nps: NPSEvent? = null, id: String = EMPTY): Boolean {
        return lifecycleScope.async(Dispatchers.IO) {
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
        lifecycleScope.launch(Dispatchers.IO) {
            val observer = Observer<WorkInfo> { workInfo ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val currentState: NPSEvent? = getCurrentNpsState()
                        if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED && currentState != null) {
                            val output = workInfo.outputData.getString("nps_question_list")
                            if (output.isNullOrEmpty().not()) {
                                NPSEventModel.removeCurrentNPA()
                                val questionList =
                                        NPSQuestionModel.getNPSQuestionModelList(output!!)
                                if (questionList.isNullOrEmpty().not()) {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        showNPSFragment(eventModel, questionList!!)
                                    }
                                    AppObjectController.appDatabase.npsEventModelDao()
                                            .insertNPSEvent(eventModel)
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
        if (isFinishing) {
            return
        }
        val prev =
                supportFragmentManager.findFragmentByTag(NetPromoterScoreFragment::class.java.name)
        if (prev != null) {
            return
        }
        val bottomSheetFragment = NetPromoterScoreFragment.newInstance(npsModel, questionList)
        bottomSheetFragment.show(supportFragmentManager, NetPromoterScoreFragment::class.java.name)
    }

    fun showSignUpDialog() {
        if (AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.FORCE_SIGN_IN_FEATURE_ENABLE)
        )
            SignUpPermissionDialogFragment.showDialog(supportFragmentManager)
    }

    fun checkForOemNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (shouldRequireCustomPermission()) {
                var oemIntent = PowerManagers.getIntentForOEM(this@BaseActivity)
                if (oemIntent == null) {
                    oemIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse(
                                    "package:$packageName"
                            )
                    )
                    oemIntent.addCategory(Intent.CATEGORY_DEFAULT)
                    oemIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    CustomPermissionDialogFragment.showCustomPermissionDialog(
                            oemIntent,
                            supportFragmentManager
                    )
                }
            }
        }
    }

    fun shouldRequireCustomPermission(): Boolean {
        val oemIntent = PowerManagers.getIntentForOEM(this)
        val performedAction = PrefManager.getStringValue(CUSTOM_PERMISSION_ACTION_KEY)
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
                .not() && oemIntent != null && performedAction == EMPTY
    }

    fun isUserProfileComplete(): Boolean {
        try {
            val user = User.getInstance()
            if (user.phoneNumber.isNullOrEmpty() && user.firstName.isNullOrEmpty()) {
                return true
            }
            if (user.firstName.isNullOrEmpty()) {
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
        if (User.getInstance().isVerified && PrefManager.getBoolValue(IS_GUEST_ENROLLED)) {
            return true
        } else if (User.getInstance().isVerified && PrefManager.getBoolValue(IS_GUEST_ENROLLED)
                        .not()
        ) {
            return false
        }
        return true
    }

    fun logout() {
        lifecycleScope.launch(Dispatchers.IO) {
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
            PrefManager.clearUser()
            AppObjectController.joshApplication.startActivity(intent)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun addInAppMessagingListener() {
        lifecycleScope.launch(Dispatchers.IO) {
            FirebaseInAppMessaging.getInstance().isAutomaticDataCollectionEnabled = true
            Firebase.inAppMessaging.addImpressionListener(this@BaseActivity)
            Firebase.inAppMessaging.addClickListener(this@BaseActivity)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun removeInAppMessagingListener() {
        FirebaseInAppMessaging.getInstance().removeImpressionListener(this)
        FirebaseInAppMessaging.getInstance().removeClickListener(this)
    }

    override fun impressionDetected(p0: InAppMessage) {
    }

    override fun messageClicked(inAppMessage: InAppMessage, action: Action) {
        lifecycleScope.launch(Dispatchers.IO) {
            Uri.parse(action.actionUrl).host?.trim()?.lowercase(Locale.getDefault()).run {
                when {
                    this == getString(R.string.conversation_open_dlink) -> {
                        val courseId = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        AppObjectController.appDatabase.courseDao().getCourseAccordingId(courseId)
                                ?.let {
                                    ConversationActivity.startConversionActivity(this@BaseActivity, it)
                                }
                    }
                    this == getString(R.string.setting_dlink) -> {
                        openSettingActivity.launch(SettingsActivity.getIntent(this@BaseActivity))
                    }
                    this == getString(R.string.referral_open_dlink) -> {
                        ReferralActivity.startReferralActivity(this@BaseActivity)
                    }
                    this == getString(R.string.reminder_open_dlink) -> {
                        startActivity(Intent(this@BaseActivity, ReminderActivity::class.java))
                    }
                    this == getString(R.string.video_open_dlink) -> {
                    }
                    this == getString(R.string.assessment_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            AssessmentActivity.startAssessmentActivity(
                                    this@BaseActivity,
                                    10001,
                                    id.toInt()
                            )
                        }
                    }
                    this == getString(R.string.landing_page_dlink) -> {
                        val id = inAppMessage.data?.getOrElse("data", { EMPTY }) ?: EMPTY
                        if (id.isNotEmpty()) {
                            CourseDetailsActivity.startCourseDetailsActivity(
                                    this@BaseActivity, id.toInt(),
                                    flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
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
                            /*    SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                                    this@BaseActivity, id
                                )*/
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
                        return@launch
                    }
                }
            }
        }
    }

    fun requestWorkerForChangeLanguage(
            lCode: String,
            successCallback: (() -> Unit)? = null,
            errorCallback: (() -> Unit)? = null,
            canCreateActivity: Boolean = true
    ) {
        val uuid = WorkManagerAdmin.getLanguageChangeWorker(lCode)
        val observer = Observer<WorkInfo> { workInfo ->
            workInfo?.run {
                if (WorkInfo.State.SUCCEEDED == state) {
                    successCallback?.invoke()
                    if (canCreateActivity) {
                        reCreateActivity()
                    }
                } else if (WorkInfo.State.FAILED == state) {
                    errorCallback?.invoke()
                }
            }
        }
        WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(uuid)
                .observe(this, observer)
    }

    protected fun reCreateActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    fun showProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            FullScreenProgressDialog.showProgressBar(this@BaseActivity)
        }
    }

    fun hideProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            FullScreenProgressDialog.hideProgressBar(this@BaseActivity)
        }
    }


    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
                lifecycleScope.launch(Dispatchers.Main) {
                    // SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
                    PointSnackbar.make(view, duration, action_lable)?.show()
                    playSnackbarSound(this@BaseActivity)
                }
            }
        }
    }

    fun showAward(awarList: List<Award>, isFromUserProfile: Boolean = false) {
        if (true) {
            // TODO add when awards functionality is over
            // if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            ShowAwardFragment.showDialog(
                    supportFragmentManager,
                    awarList,
                    isFromUserProfile
            )
        }
    }

    fun showWebViewDialog(webUrl: String) {
        WebViewFragment.showDialog(supportFragmentManager, webUrl)
    }

    fun showLeaderboardAchievement(
            outrankData: OutrankedDataResponse,
            lessonInterval: Int,
            chatId: String,
            lessonNo: Int
    ) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            // if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            ShowAnimatedLeaderBoardFragment.showDialog(
                    supportFragmentManager,
                    outrankData, lessonInterval, chatId, lessonNo
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SHOW_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                startLocationUpdates()
            } else {
                onDenyLocation()
            }
        }
    }

    private var locationUpdatesJob: Job? = null
    private val coLocation: CoLocation by lazy {
        CoLocation.from(applicationContext)
    }
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
    }

    @SuppressLint("MissingPermission")
    protected fun fetchUserLocation() {
        lifecycleScope.launch(Dispatchers.IO) {
            when (val settingsResult = coLocation.checkLocationSettings(locationRequest)) {
                CoLocation.SettingsResult.Satisfied -> {
                    val location = coLocation.getLastLocation()
                    if (null == location) {
                        startLocationUpdates()
                    } else {
                        onUpdateLocation(location)
                    }
                }
                is CoLocation.SettingsResult.Resolvable -> {
                    settingsResult.resolve(this@BaseActivity, REQUEST_SHOW_SETTINGS)
                }
                else -> { /* Ignore for now, we can't resolve this anyway */
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            locationUpdatesJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        locationUpdatesJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                coLocation.getLocationUpdates(locationRequest).collectLatest {
                    onUpdateLocation(it)
                    locationUpdatesJob?.cancel()
                }
            } catch (e: CancellationException) {
                e.printStackTrace()
            }
        }
    }

    protected suspend fun uploadUserLocation(location: Location) {
        try {
            val request = UpdateUserLocality()
            request.locality =
                    SearchLocality(location.latitude, location.longitude)
            AppAnalytics.setLocation(
                    location.latitude,
                    location.longitude
            )
            val response: ProfileResponse =
                    AppObjectController.signUpNetworkService.updateUserAddressAsync(
                            Mentor.getInstance().getId(), request
                    )
            Mentor.getInstance().setLocality(response.locality).update()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun onUpdateLocation(location: Location) {}
    open fun onDenyLocation() {}
}
