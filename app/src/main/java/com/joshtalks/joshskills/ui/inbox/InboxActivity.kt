package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.MINIMUM_TIME_TO_SHOW_REVIEW
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.SubscriptionData
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.extra.TopTrialTooltipView
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.translation.LanguageTranslation
import com.joshtalks.joshskills.ui.translation.RelativePopupWindow
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.rxlocation.RxLocation
import io.agora.rtc.RtcEngine
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.android.synthetic.main.inbox_toolbar.*
import kotlinx.android.synthetic.main.top_free_trial_expire_time_tooltip_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_NEW = 2008
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"
const val IS_FROM_NEW_ONBOARDING = "is_from_new_on_boarding_flow"

class InboxActivity : CoreJoshActivity(), LifecycleObserver, InAppUpdateManager.InAppUpdateHandler {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()
    private var inAppUpdateManager: InAppUpdateManager? = null
    private lateinit var reminderIv: ImageView
    private lateinit var findMoreLayout: View
    lateinit var countdown_timer: CountDownTimer
    var isRunning: Boolean = false
    var isFromOnBoarding: Boolean = false
    var time_in_milli_seconds = 0L
    var expiryToolText: String = EMPTY
    private lateinit var popupMenu: PopupMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        //WebRtcService.loginUserClient()
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        AppObjectController.isSettingUpdate = false
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        setToolbar()
        addLiveDataObservable()
        checkAppUpdate()
        viewModel.updateSubscriptionStatus()
        workInBackground()
        handelIntentAction()
        initNewUserTip()
        viewModel.getTotalWatchTime()
        //viewModel.getProfileData(Mentor.getInstance().getId())
    }

    private fun initNewUserTip() {
        if (isFromOnBoarding) {
            val boolean = AppObjectController.getFirebaseRemoteConfig()
                .getBoolean(FirebaseRemoteConfigKey.SHOW_BB_TOOL_TIP_FIRST_TIME)
            if (boolean) {
                new_user_layout.visibility = View.VISIBLE
                hint_text.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_FIRST_TIME_TEXT)
                val content = SpannableString(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_FIRST_TIME_BTN_TEXT)
                )
                content.setSpan(UnderlineSpan(), 0, content.length, 0)
                text_btn.text = content
                new_user_layout.setOnClickListener {
                    logEvent(AnalyticsEvent.BLANK_INBOX_SCREEN_CLICKED.NAME)
                    new_user_layout.visibility = View.GONE
                    viewModel.logInboxEngageEvent()
                }
                text_btn.setOnClickListener {
                    logEvent(AnalyticsEvent.OK_GOT_IT_CLICKED.NAME)
                    new_user_layout.visibility = View.GONE
                    viewModel.logInboxEngageEvent()
                }
            }
        }
    }

    private fun showExpiryTimeToolTip() {
        expiry_tool_tip.visibility = View.VISIBLE
        startThreadForTextUpdate()
    }

    private fun startThreadForTextUpdate() {
        val freeTrialData = FreeTrialData.getMapObject()
        time_in_milli_seconds = (1000 * 60)
        freeTrialData?.let { data ->
            time_in_milli_seconds = data.endDate?.minus(data.today)?.times(1000) ?: (1000 * 60)
        }
        startTimer(time_in_milli_seconds)
    }

    private fun startTimer(time_in_seconds: Long) {
        countdown_timer = object : CountDownTimer(time_in_seconds, 1000) {
            override fun onFinish() {
                if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED).not()) {
                    PrefManager.put(IS_TRIAL_ENDED, true)
                    expiry_tool_tip_text.text = getString(R.string.free_trial_completed)
                }
            }

            override fun onTick(p0: Long) {
                time_in_milli_seconds = p0
                updateTextUI(time_in_milli_seconds)
            }
        }
        countdown_timer.start()
        isRunning = true
    }

    private fun updateTextUI(millis: Long) {
        val days = TimeUnit.MILLISECONDS.toDays(time_in_milli_seconds).toInt()
        if (days == 0) {
            expiryToolText =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_EXPIRY_TEXT)
        } else if (days == 1) {
            expiryToolText =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_EXPIRY_TEXT)
                    .plus(" ${days} Day")
        } else {
            expiryToolText =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_EXPIRY_TEXT)
                    .plus(" $days Days")
        }

        val time = String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis).rem(24),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )

        expiry_tool_tip_text.text = "${expiryToolText.plus(SINGLE_SPACE)}$time"
    }

    private fun updateSubscriptionTipView(
        exploreType: ExploreCardType,
        showTooltip1: Boolean,
        showTooltip2: Boolean
    ) {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED).not()) {
            when (exploreType) {
                ExploreCardType.FREETRIAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE

                    val remainingTrialDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS)
                    if (remainingTrialDays in 0..7 && showTooltip1) {
                        showToolTipBelowFindMoreCourse(remainingTrialDays)
                    }
                    if (remainingTrialDays in 0..4 && showTooltip2) {
                        showExpiryTimeToolTip()
                    }
                    txtSubscriptionTip.isSelected = true
                    when {
                        remainingTrialDays <= 0 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)
                        }

                        remainingTrialDays == 1 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)

                        }

                        remainingTrialDays == 2 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)

                        }

                        remainingTrialDays == 3 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)

                        }

                        remainingTrialDays == 4 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)
                        }

                        remainingTrialDays == 5 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)
                        }

                        remainingTrialDays == 6 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)
                        }

                        remainingTrialDays > 6 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)
                        }
                    }
                }

                ExploreCardType.FFCOURSE -> {
                    subscriptionTipContainer.visibility = View.VISIBLE
                    viewModel.registerCourseNetworkLiveData.value?.let {
                        txtSubscriptionTip.text = if (it.size > 1) {
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                        } else {
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_FFCOURSE_TIP)
                        }
                        txtSubscriptionTip2.text = if (it.size > 1) {
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                        } else {
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_FFCOURSE_TIP)
                        }
                    }
                }

                ExploreCardType.NORMAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE
                    viewModel.registerCourseNetworkLiveData.value?.let {
                        txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                        txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                    }.run {
                        subscriptionTipContainer.visibility = View.GONE
                    }
                }

            }
        } else {
            subscriptionTipContainer.visibility = View.GONE
        }

    }

    private fun showToolTipBelowFindMoreCourse(remainingTrialDays: Int) {
        bb_tip_below_find_btn.visibility = View.VISIBLE
        (bb_tip_below_find_btn as TopTrialTooltipView).setFindMoreCourseTipText(remainingTrialDays)
    }

    private fun setCTAButtonText(exploreType: ExploreCardType) {
        if (isGuestUser()) {
            find_more.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.INBOX_SCREEN_CTA_TEXT_ONBOARD_FLOW)
        } else {
            find_more.text = when (exploreType) {

                ExploreCardType.NORMAL ->
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.INBOX_SCREEN_CTA_TEXT_NORMAL)

                ExploreCardType.FFCOURSE ->
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.INBOX_SCREEN_CTA_TEXT_FFCOURSE)

                ExploreCardType.FREETRIAL ->
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.INBOX_SCREEN_CTA_TEXT_FREETRIAL)

            }
        }
    }

    private fun showInAppReview() {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = request.result
                val flow = manager.launchReviewFlow(this@InboxActivity, reviewInfo)
                flow.addOnCompleteListener { result ->

                    println("result = [${result.isSuccessful}]")
                    result.exception?.printStackTrace()
                }
            }
        }
    }

    private fun setToolbar() {
        iv_reminder.visibility = View.GONE
        iv_setting.visibility = View.VISIBLE
        text_message_title.text = getString(R.string.inbox_header)
        findMoreLayout = findViewById(R.id.parent_layout)
        if (isGuestUser()) {
            if (VersionResponse.getInstance().hasVersion()) {
                when (VersionResponse.getInstance().version!!.name) {
                    ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                        find_more.setOnClickListener {
                            AppAnalytics.create(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .push()
                            RxBus2.publish(ExploreCourseEventBus())
                        }
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                        find_more.text = getString(R.string.add_more_courses)
                        find_more.setOnClickListener {
                            AppAnalytics.create(AnalyticsEvent.ADD_MORE_COURSE_CLICKED.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .addParam(
                                    "version",
                                    VersionResponse.getInstance().version?.name.toString()
                                )
                                .push()
                            if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED) && PrefManager.getBoolValue(
                                    IS_SUBSCRIPTION_ENDED
                                ).not()
                            ) {
                                openCourseExplorer()
                            } else {
                                openCourseSelectionExplorer(true)
                            }
                        }
                    }
                }
            }
        } else {
            find_more.setOnClickListener {
                AppAnalytics.create(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
                RxBus2.publish(ExploreCourseEventBus())
            }
        }
        findViewById<View>(R.id.iv_setting).setOnClickListener {
            openPopupMenu(it)
        }
        reminderIv = findViewById<ImageView>(R.id.iv_reminder)
        reminderIv.visibility = View.GONE
        reminderIv.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.REMINDER_BELL_CLICKED.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()

            viewModel.getTotalRemindersFromLocal()
        }
        if (!SharedPrefsManager.newInstance(this)
                .getBoolean(SharedPrefsManager.Companion.IS_REMINDER_SYNCED, false)
        ) {
            viewModel.getRemindersFromServer()
        } else
            animateBell()
    }

    fun animateBell() {
        if (SharedPrefsManager.newInstance(this)
                .getBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, false)
        ) {
            val shake: Animation = AnimationUtils.loadAnimation(this, R.anim.shake_animation)
            reminderIv.animation = shake
        } else {
            reminderIv.clearAnimation()
        }
    }

    private fun openReminderCallback(reminderListSize: Int?) {
        if (reminderListSize != null && reminderListSize > 0) {
            startActivity(
                Intent(
                    applicationContext,
                    ReminderListActivity::class.java
                )
            )
        } else startActivity(
            Intent(
                applicationContext,
                ReminderActivity::class.java
            )
        )
    }

    private fun openPopupMenu(view: View) {
        popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
        popupMenu.inflate(R.menu.more_options_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_referral -> {
                    AppAnalytics
                        .create(AnalyticsEvent.REFER_BUTTON_CLICKED.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(
                            AnalyticsEvent.REFERRAL_CODE.NAME,
                            Mentor.getInstance().referralCode
                        )
                        .push()
                    ReferralActivity.startReferralActivity(
                        this@InboxActivity,
                        InboxActivity::class.java.name
                    )
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_help -> {
                    openHelpActivity()
                }
                R.id.menu_settings ->
                    openSettingActivity()
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }

    private fun openSettingActivity() {
        openSettingActivity.launch(SettingsActivity.getIntent(this))
    }

    private fun workInBackground() {
        CoroutineScope(Dispatchers.Default).launch {
            processIntent(intent)
            WorkManagerAdmin.determineNPAEvent()
        }
        when {
            /*  PrefManager.getBoolValue(BETTERY_OPTIMIZATION_ALREADY_ASKED).not() -> {
                  PowerManagers.checkIgnoreBatteryOptimization(this)
                  PrefManager.put(BETTERY_OPTIMIZATION_ALREADY_ASKED, true)
              }*/
            NPSEventModel.getCurrentNPA() != null -> {
                showNetPromoterScoreDialog()
            }

            else -> {
                viewModel.registerCourseMinimalLiveData.value?.run {
                    if (this.isNotEmpty()) {
                        locationFetch()
                    }
                }
            }
        }
    }


    private fun checkAppUpdate() {
        val forceUpdateMinVersion =
            AppObjectController.getFirebaseRemoteConfig().getLong("force_upgrade_after_version")
        val forceUpdateFlag =
            AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")
        val currentAppVersion = com.joshtalks.joshskills.BuildConfig.VERSION_CODE
        var updateMode = Constants.UpdateMode.FLEXIBLE

        if (currentAppVersion <= forceUpdateMinVersion && forceUpdateFlag) {
            updateMode = Constants.UpdateMode.IMMEDIATE
        }
        inAppUpdateManager = InAppUpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
            .resumeUpdates(true)
            .mode(updateMode)
            .useCustomNotification(false)
            .snackBarMessage(getString(R.string.update_message))
            .snackBarAction(getString(R.string.restart))
            .handler(this)

        inAppUpdateManager?.checkForAppUpdate()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handelIntentAction()
    }

    private fun handelIntentAction() {
        if (intent != null && intent.hasExtra(ACTION_TYPE)) {
            val obj = intent.getSerializableExtra(ACTION_TYPE) as NotificationAction?
            obj?.let {
                if (NotificationAction.ACTION_UP_SELLING_POPUP == it) {
                    showPromotionScreen(
                        intent.getStringExtra(COURSE_ID)!!,
                        intent.getStringExtra(ARG_PLACEHOLDER_URL)!!
                    )
                }
            }
        }
        if (intent != null && intent.hasExtra(IS_FROM_NEW_ONBOARDING)) {
            isFromOnBoarding = intent.getBooleanExtra(IS_FROM_NEW_ONBOARDING, false)
        }
    }

    private fun locationFetch() {
        if (Mentor.getInstance().getLocality() == null) {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                getLocationAndUpload()
                            }
                        }
                    }

                }).check()
        }
    }


    private fun addLiveDataObservable() {
        viewModel.registerCourseNetworkLiveData.observe(this) {
            if (it == null || it.isEmpty()) {
                if (isGuestUser()) {
                    openNewOnBoardFlow()
                } else {
                    openCourseExplorer()
                }
            } else {
                addCourseInRecyclerView(it)
            }
        }
        viewModel.onBoardingLiveData.observe(this) {
            setTrialEndParam()
            setSubscriptionEndParam()
            val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, false)
            val exploreType =
                if (exploreTypeStr.isNotBlank()) ExploreCardType.valueOf(exploreTypeStr) else ExploreCardType.NORMAL
            updateSubscriptionTipView(
                exploreType,
                it.showTooltip1,
                it.showTooltip2
            )
            setCTAButtonText(exploreType)

            val showOverlay = intent.getBooleanExtra(
                SHOW_OVERLAY,
                false
            ) && it.subscriptionData.isSubscriptionBought?.not() ?: false
            if (showOverlay && it.showTooltip3 && it.freeTrialData.remainingDays in 4..7) {
                showOverlayToolTip(it.freeTrialData.remainingDays)
            } else {
                overlay_layout.visibility = View.GONE
            }

            if (User.getInstance().isVerified.not() && it?.subscriptionData?.isSubscriptionBought == true) {
                showSignUpDialog()
            }

        }

        viewModel.registerCourseMinimalLiveData.observe(this) {
            addCourseInRecyclerView(it)
        }

        txtConvert.setOnClickListener {
            logEvent(AnalyticsEvent.CONVERT_CLICKED.name)
            PaymentSummaryActivity.startPaymentSummaryActivity(
                this, PrefManager.getIntValue(SUBSCRIPTION_TEST_ID).toString()
            )
        }
        txtConvert2.setOnClickListener {
            overlay_layout.visibility = View.GONE
            logEvent(AnalyticsEvent.CONVERT_CLICKED.name)
            PaymentSummaryActivity.startPaymentSummaryActivity(
                this, PrefManager.getIntValue(SUBSCRIPTION_TEST_ID).toString()
            )
        }

        overlay_layout.setOnClickListener {
            overlay_layout.visibility = View.GONE
        }

        viewModel.reminderApiCallStatusLiveData.observe(this, {
            if (ApiCallStatus.SUCCESS == it) {
                animateBell()
            }
        })

        viewModel.totalRemindersLiveData.observe(this) {
            openReminderCallback(it)
        }

        viewModel.overAllWatchTime.observe(this, {
            var reviewCount = PrefManager.getIntValue(IN_APP_REVIEW_COUNT)
            val reviewFrequency =
                AppObjectController.getFirebaseRemoteConfig().getLong(MINIMUM_TIME_TO_SHOW_REVIEW)
            when (reviewCount) {
                0 -> if (it > reviewFrequency) {
                    showInAppReview()
                    PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                }
                1 -> if (it > reviewFrequency * 2) {
                    PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                    showInAppReview()
                }
                2 -> if (it > reviewFrequency * 3) {
                    PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                    showInAppReview()
                }
            }
        })
        viewModel.userData.observe(this, {
            it?.let {
                ///hideProgressBar()
                initScoreCardView(it)
            }
        })
    }

    private fun initScoreCardView(userData: UserProfileResponse) {
        userData.isPointsActive?.let { isLeaderBoardActive ->
            PrefManager.put(IS_LEADERBOARD_ACTIVE, userData.isPointsActive)
            if (AppObjectController.getFirebaseRemoteConfig()
                    .getBoolean(FirebaseRemoteConfigKey.SHOW_AWARDS_FULL_SCREEN)
            ) {
                var unseenAwards: ArrayList<Award>? = ArrayList()
                userData.awardCategory?.forEach {
                    it.awards?.filter { it.isSeen == false && it.is_achieved == true }
                        ?.forEach {
                            unseenAwards?.add(it)
                        }
                }
                if (unseenAwards.isNullOrEmpty().not()) {
                    showAward(unseenAwards?.toList()!!)
                }
            }
        }
    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .addParam("version", VersionResponse.getInstance().version?.name.toString())
            .push()
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>?) {
        if (items.isNullOrEmpty()) {
            return
        }
        recycler_view_inbox.removeAllViews()
        val total = items.size
        var capsuleIndex = 0
        val newCourses = items.filter {
            (it.created == null || it.created == 0L) && it.courseId.equals("151").not()
        }
        val capsuleCourse = items.filter { it.courseId.equals("151") }
        if (capsuleCourse.isNullOrEmpty().not()) {
            recycler_view_inbox.addView(
                InboxViewHolder(
                    capsuleCourse.get(0), total, 0
                )
            )
            capsuleIndex = 1
        }


        newCourses.sortedByDescending { it.courseCreatedDate }.forEachIndexed { index, inbox ->
            if (inbox.courseId != TRIAL_COURSE_ID && inbox.courseId.equals("151").not())
                recycler_view_inbox.addView(
                    InboxViewHolder(
                        inbox, total, index + capsuleIndex
                    )
                )
        }
        items.filter { it.created != null && it.created != 0L && it.courseId.equals("151").not() }
            .sortedByDescending { it.created }
            .forEachIndexed { index, inbox ->
                if (inbox.courseId != TRIAL_COURSE_ID)
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox, total, newCourses.size + index + capsuleIndex
                        )
                    )
            }
        progress_bar.visibility = View.GONE
        findMoreLayout.visibility = View.VISIBLE
    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(OpenCourseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ConversationActivity.startConversionActivity(this, it.inboxEntity)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(RxBus2.listen(ExploreCourseEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                openCourseExplorer()
            })
        compositeDisposable.add(RxBus2.listen(NPSEventGenerateEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                showNetPromoterScoreDialog()
            })

        compositeDisposable.add(RxBus2.listen(OpenLeaderBoardEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                openLeaderBoard()
            })
        compositeDisposable.add(RxBus2.listen(OpenUserProfileEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                openUserProfileActivity(Mentor.getInstance().getId())
            })

    }

    private fun openNewOnBoardFlow() {
        if (VersionResponse.getInstance().hasVersion()) {
            when (VersionResponse.getInstance().version?.name) {
                ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                    openCourseExplorer()
                }
                ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                    openCourseSelectionExplorer()
                }
                else -> {
                    openCourseExplorer()
                }
            }
        }
    }

    private fun openCourseExplorer() {
        val registerCourses: MutableSet<InboxEntity> = mutableSetOf()
        viewModel.registerCourseMinimalLiveData.value?.let {
            registerCourses.addAll(it)
        }
        viewModel.registerCourseNetworkLiveData.value?.let {
            registerCourses.addAll(it)
        }
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            registerCourses, state = ActivityEnum.Inbox
        )
    }

    private fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean = false) {
        OnBoardingActivityNew.startOnBoardingActivity(
            this,
            COURSE_EXPLORER_NEW,
            true,
            alreadyHaveCourses
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_INFO_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                overridePendingTransition(0, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            } else {
                finish()
            }
        } else if (requestCode == REQ_CODE_VERSION_UPDATE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                val forceUpdateMinVersion =
                    AppObjectController.getFirebaseRemoteConfig()
                        .getLong("force_upgrade_after_version")
                val forceUpdateFlag =
                    AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")
                val currentAppVersion = BuildConfig.VERSION_CODE

                if (currentAppVersion <= forceUpdateMinVersion && forceUpdateFlag) {
                    inAppUpdateManager?.checkForAppUpdate()
                }
            }
        } else if (requestCode == COURSE_EXPLORER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                overridePendingTransition(0, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            } else if (resultCode == Activity.RESULT_CANCELED && viewModel.registerCourseNetworkLiveData.value.isNullOrEmpty()) {
                if ((viewModel.registerCourseMinimalLiveData.value.isNullOrEmpty())) {
                    this@InboxActivity.finish()
                }
            }
        } else if (requestCode == USER_DETAILS_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(application)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)

        rxLocation.settings().checkAndHandleResolutionCompletable(locationRequest)
            .subscribeOn(Schedulers.computation())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    compositeDisposable.add(
                        rxLocation.location().updates(locationRequest)
                            .subscribeOn(Schedulers.computation())
                            .subscribe({ location ->
                                CoroutineScope(Dispatchers.IO).launch {
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
                                                Mentor.getInstance().getId(),
                                                request
                                            ).await()
                                        Mentor.getInstance().setLocality(response.locality).update()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    compositeDisposable.clear()
                                }
                            }, { _ ->
                            })
                    )
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
        viewModel.getRegisterCourses()
        viewModel.getProfileData(Mentor.getInstance().getId())
        animateBell()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onInAppUpdateError(code: Int, error: Throwable?) {
        error?.printStackTrace()
    }

    override fun onInAppUpdateStatus(status: InAppUpdateStatus?) {
        if (status != null && status.isDownloaded) {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            val snackBar = Snackbar.make(
                rootView,
                getString(R.string.update_download_success_message),
                Snackbar.LENGTH_INDEFINITE
            )
            snackBar.setAction(getString(R.string.restart)) {
                inAppUpdateManager?.completeUpdate()
            }
            snackBar.show()
        }
    }

    private fun setTrialEndParam() {
        val freeTrialData = FreeTrialData.getMapObject()
        freeTrialData?.let {
            if (it.is7DFTBought == false) {
                PrefManager.put(IS_TRIAL_ENDED, false, false)
                PrefManager.put(IS_TRIAL_STARTED, false, false)
                PrefManager.put(REMAINING_TRIAL_DAYS, -1)
            } else {
                (it.remainingDays < 0).let { trialExpired ->
                    PrefManager.put(
                        IS_TRIAL_ENDED,
                        trialExpired,
                        false
                    )

                    if (trialExpired)
                        logTrialEventExpired()
                }

                PrefManager.put(
                    IS_TRIAL_STARTED,
                    it.is7DFTBought ?: false, false
                )

                PrefManager.put(REMAINING_TRIAL_DAYS, it.remainingDays)
            }
        }
    }

    private fun setSubscriptionEndParam() {
        val subscriptionData = SubscriptionData.getMapObject()
        subscriptionData?.let {
            if (it.isSubscriptionBought == false) {
                PrefManager.put(IS_SUBSCRIPTION_ENDED, false, false)
                PrefManager.put(IS_SUBSCRIPTION_STARTED, false, false)
                PrefManager.put(REMAINING_SUBSCRIPTION_DAYS, -1, false)
            } else {
                (it.remainingDays < 0).let { subsEnded ->
                    PrefManager.put(
                        IS_SUBSCRIPTION_ENDED,
                        subsEnded,
                        false
                    )
                    if (subsEnded)
                        logSubscriptionExpired()
                }
                PrefManager.put(
                    IS_SUBSCRIPTION_STARTED, it.isSubscriptionBought ?: false, false
                )
                PrefManager.put(
                    REMAINING_SUBSCRIPTION_DAYS, it.remainingDays,
                    false
                )
            }
        }
    }

    private fun logTrialEventExpired() {
        if (PrefManager.getBoolValue(IS_TRIAL_ENDED, false).not()) {
            AppAnalytics.create(AnalyticsEvent.SEVEN_DAY_TRIAL_OVER.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
        }
    }

    private fun logSubscriptionExpired() {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED, false).not()) {
            AppAnalytics.create(AnalyticsEvent.SUBSCRIPTION_EXPIRED.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
        }
    }

    private fun showOverlayToolTip(remainingTrialDays: Int) {
        overlay_layout.visibility = View.VISIBLE
        (overlay_tip as TopTrialTooltipView).setInboxOverayTipText(7.minus(remainingTrialDays))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (WebRtcService.isCallWasOnGoing.not()) {
            RtcEngine.destroy()
        }
    }
}
