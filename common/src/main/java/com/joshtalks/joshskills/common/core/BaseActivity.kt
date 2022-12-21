package com.joshtalks.joshskills.common.core

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.android.installreferrer.api.InstallReferrerClient
import com.google.android.gms.location.LocationRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener
import com.google.firebase.inappmessaging.ktx.inAppMessaging
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.voip.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.voip.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.voip.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.common.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.common.ui.gif.GIFActivity
import com.joshtalks.joshskills.common.core.analytics.*
import com.joshtalks.joshskills.common.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.common.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.common.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.repository.server.OutrankedDataResponse
import com.joshtalks.joshskills.common.repository.server.ProfileResponse
import com.joshtalks.joshskills.common.repository.server.SearchLocality
import com.joshtalks.joshskills.common.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.common.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.common.ui.chat.ConversationActivity
import com.joshtalks.joshskills.common.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.common.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.common.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.common.ui.extra.SignUpPermissionDialogFragment
import com.joshtalks.joshskills.common.ui.help.HelpActivity
import com.joshtalks.joshskills.common.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.common.ui.inbox.InboxActivity
import com.joshtalks.joshskills.common.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.common.ui.points_history.PointsHistoryActivity
import com.joshtalks.joshskills.common.ui.points_history.SpokenHistoryActivity
import com.joshtalks.joshskills.common.ui.referral.ReferralActivity
import com.joshtalks.joshskills.common.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.common.ui.termsandconditions.WebViewFragment
//import com.joshtalks.joshskills.userprofile.fragments.ShowAwardFragment
import com.joshtalks.joshskills.common.ui.userprofile.models.Award
import com.patloew.colocation.CoLocation
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.lang.reflect.Type
import java.util.*

const val HELP_ACTIVITY_REQUEST_CODE = 9010
const val COURSE_EXPLORER_NEW = 2008
const val REQUEST_SHOW_SETTINGS = 123

abstract class BaseActivity :
        com.joshtalks.joshskills.common.track.TrackActivity(),
        LifecycleObserver,
        FirebaseInAppMessagingImpressionListener,
        FirebaseInAppMessagingClickListener {

    private lateinit var referrerClient: InstallReferrerClient
    private val versionResponseTypeToken: Type = object : TypeToken<VersionResponse>() {}.type
    private var versionResponse: VersionResponse? = null
    var videoChatObject: ChatModel? = null

    enum class ActivityEnum {
        Conversation,
        CourseExplore,
        Help,
        Inbox,
        Launcher,
        CourseDetails,
        Onboard,
        Signup,
        Empty,
        DeepLink,
        FreeTrial,
        BuyPage
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


    private fun initIdentifierForTools() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                try {
                    Branch.getInstance().setIdentity(PrefManager.getStringValue(USER_UNIQUE_ID))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                initNewRelic()
            }
        }
    }

    fun openHelpActivity() {
        val i = Intent(this, HelpActivity::class.java)
        startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
    }

    fun openGifActivity(conversationId: String?) {
        startActivity(
                Intent(this, GIFActivity::class.java).apply {
                    putExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID, conversationId)
                }
        )
    }
    //TODO: fix this
    fun openLeaderBoard(conversationId: String, courseId: String?) {
        /*val i = Intent(this, com.joshtalks.joshskills.leaderboard.LeaderBoardViewPagerActivity::class.java).apply {
            putExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID, conversationId)
            putExtra(COURSE_ID, courseId)
        }
        startActivity(i)*/
    }

    fun openPointHistory(mentorId: String? = null, conversationId: String? = null) {
        PointsHistoryActivity.startPointHistory(this, mentorId, conversationId)
    }

    fun openSpokenMinutesHistory(mentorId: String? = null, conversationId: String? = null) {
        SpokenHistoryActivity.startSpokenMinutesHistory(this, mentorId, conversationId)
    }

    fun getActivityType(act: Activity): ActivityEnum {
        return when (act) {
            is ConversationActivity -> ActivityEnum.Conversation
            is CourseExploreActivity -> ActivityEnum.CourseExplore
            is HelpActivity -> ActivityEnum.Help
            is InboxActivity -> ActivityEnum.Inbox
            is LauncherActivity -> ActivityEnum.Launcher
            is CourseDetailsActivity -> ActivityEnum.CourseDetails
            //is com.joshtalks.joshskills.auth.freetrail.SignUpActivity -> ActivityEnum.Signup
            else -> ActivityEnum.Empty
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

    fun getInboxActivityIntent(isFromOnBoardingFlow: Boolean = false): Intent {
        return Intent(applicationContext, InboxActivity::class.java).apply {
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
            // TODO: Variables added, to be checked -- Sukesh
            val HAS_NOTIFICATION = "has_notification"
            val NOTIFICATION_ID = "notification_id"
            lifecycleScope.launch(Dispatchers.IO) {
                if (mIntent != null && mIntent.hasExtra(HAS_NOTIFICATION) &&
                    mIntent.hasExtra(NOTIFICATION_ID) &&
                    mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not()
                ) {
                    // TODO: (IMP) Uncomment code -- Sukesh
//                    NotificationAnalytics().addAnalytics(
//                        notificationId = mIntent.getStringExtra(NOTIFICATION_ID)!!,
//                        mEvent = NotificationAnalytics.Action.CLICKED,
//                        channel = null
//                    )
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

    fun showSignUpDialog() {
        if (AppObjectController.getFirebaseRemoteConfig()
                .getBoolean(FirebaseRemoteConfigKey.FORCE_SIGN_IN_FEATURE_ENABLE)
        )
            SignUpPermissionDialogFragment.showDialog(supportFragmentManager)
    }

    fun checkForOemNotifications(event: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var oemIntent = PowerManagers.getIntentForOEM(this@BaseActivity)
            if (oemIntent == null) {
                oemIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                oemIntent.addCategory(Intent.CATEGORY_DEFAULT)
                oemIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            lifecycleScope.launch(Dispatchers.Main) {
                CustomPermissionDialogFragment.showCustomPermissionDialog(
                    oemIntent,
                    supportFragmentManager,
                    event
                )
            }
        }
    }

    fun shouldRequireCustomPermission(): Boolean {
        val oemIntent = PowerManagers.getIntentForOEM(this)
        return isNotificationEnabled() && oemIntent != null
    }

    fun isNotificationEnabled() =
        NotificationManagerCompat.from(this).areNotificationsEnabled().not()

    fun pushAnalyticsToServer(eventName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
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

    fun isRegProfileComplete(): Boolean {
        val user = User.getInstance()
        return (!user.firstName.isNullOrEmpty() && !(user.phoneNumber.isNullOrEmpty() && user.email.isNullOrEmpty()) && !user.dateOfBirth.isNullOrEmpty() && !user.gender.isNullOrEmpty())
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
            MixPanelTracker.publishEvent(MixPanelEvent.USER_LOGGED_OUT).push()
            AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                    .addUserDetails()
                    .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, true).push()
            //TODO: Uncomment (IMP) -- Sukesh
//            PubNubService.cancelAllPubNubNotifications()
            //TODO: Replace AppObjectController with intent navigator -- Sukesh
            AppObjectController.navigator.with(this@BaseActivity).navigate(
                object : SignUpContract {
                    override val flowFrom = "UserLogout"
                    override val navigator = AppObjectController.navigator
                }
            )
            //TODO: add flags for the above contract
//            intent.apply {
//                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            }

            try {
                AppObjectController.signUpNetworkService.signoutUser(Mentor.getInstance().getId())
                val broadcastIntent = Intent().apply {
                    action =
                        CALLING_SERVICE_ACTION
                    putExtra(
                        SERVICE_BROADCAST_KEY,
                        STOP_SERVICE
                    )
                }
                LocalBroadcastManager.getInstance(this@BaseActivity).sendBroadcast(broadcastIntent)

                PrefManager.logoutUser()
                NotificationManagerCompat.from(this@BaseActivity).cancelAll()
                AppObjectController.joshApplication.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Something went wrong. Please try again.")
            }
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
                        AppObjectController.appDatabase.courseDao().getCourseFromId(courseId)
                            ?.let {
                                ConversationActivity.startConversionActivity(this@BaseActivity, it)
                            }
                    }
                    this == getString(R.string.setting_dlink) -> {
                        //TODO: Replace AppObjectController with intent navigator -- Sukesh
                        AppObjectController.navigator.with(this@BaseActivity).navigate(
                            object : SettingsContract {
                                override val navigator = AppObjectController.navigator
                            }
                        )
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
    //TODO Make navigation to Open ShowAwardFragment
    fun showAward(awarList: List<Award>, isFromUserProfile: Boolean = false) {
        if (true) {
            // TODO add when awards functionality is over
            // if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
//            com.joshtalks.joshskills.userprofile.fragments.ShowAwardFragment.showDialog(
//                supportFragmentManager,
//                awarList,
//                isFromUserProfile
//            )
        }
    }

    fun showWebViewDialog(webUrl: String) {
        WebViewFragment.showDialog(supportFragmentManager, webUrl)
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
