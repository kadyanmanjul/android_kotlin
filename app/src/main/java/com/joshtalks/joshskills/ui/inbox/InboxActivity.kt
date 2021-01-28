package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.*
import com.google.android.gms.location.LocationRequest
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.observe
import com.google.android.gms.location.LocationRequest
import com.cometchat.pro.constants.CometChatConstants
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenLeaderBoardEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.SubscriptionData
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.adapter.InboxAdapter
import com.joshtalks.joshskills.ui.inbox.extra.TopTrialTooltipView
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.util.FileUploadService
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.RtcEngine
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.android.synthetic.main.fragment_listen_practise.*
import kotlinx.android.synthetic.main.inbox_toolbar.*
import kotlinx.android.synthetic.main.top_free_trial_expire_time_tooltip_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"
const val IS_FROM_NEW_ONBOARDING = "is_from_new_on_boarding_flow"

class InboxActivity : InboxBaseActivity(), LifecycleObserver {

    private var popupMenu: PopupMenu? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var findMoreLayout: View
    lateinit var countdown_timer: CountDownTimer
    var isRunning: Boolean = false
    var time_in_milli_seconds = 0L
    var expiryToolText: String = EMPTY
    private val courseListSet: MutableSet<InboxEntity> = hashSetOf()
    private val inboxAdapter: InboxAdapter by lazy { InboxAdapter(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        /*  if (intent != null && intent.hasExtra(StringContract.IntentStrings.GUID)) {
              val groupId = intent.getStringExtra(StringContract.IntentStrings.GUID)
              viewModel.groupIdLiveData.observe(this, Observer {
                  startActivity(
                      Intent(applicationContext, CometChatMessageListActivity::class.java).apply {
                          putExtra(HAS_NOTIFICATION, true)
                          putExtra(StringContract.IntentStrings.GUID, it)
                          putExtra(
                              StringContract.IntentStrings.TYPE,
                              CometChatConstants.RECEIVER_TYPE_GROUP
                          )
                      }
                  )
              })
              viewModel.initCometChat(groupId)
          }*/
        AppObjectController.isSettingUpdate = false
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        initView()
        addLiveDataObservable()
        viewModel.updateSubscriptionStatus()
        workInBackground()
        handelIntentAction()
        initNewUserTip()
        viewModel.getTotalWatchTime()
        FileUploadService.uploadAllPendingTasks(AppObjectController.joshApplication)
        //viewModel.getProfileData(Mentor.getInstance().getId())

    }

    private fun initView() {
        text_message_title.text = getString(R.string.inbox_header)
        iv_reminder.visibility = View.GONE
        iv_setting.visibility = View.VISIBLE
        findMoreLayout = findViewById(R.id.parent_layout)
        recycler_view_inbox.adapter = inboxAdapter
        recycler_view_inbox.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        recycler_view_inbox.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        recycler_view_inbox.layoutManager = SmoothLinearLayoutManager(applicationContext)
        recycler_view_inbox.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    applicationContext,
                    6f
                )
            )
        )


        iv_setting.setOnClickListener {
            openPopupMenu(it)
        }
        find_more.setOnClickListener {
            courseExploreClick()
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

    private fun initView() {
        text_message_title.text = getString(R.string.inbox_header)
        iv_reminder.visibility = View.GONE
        iv_setting.visibility = View.VISIBLE
        findMoreLayout = findViewById(R.id.parent_layout)
        recycler_view_inbox.adapter = inboxAdapter
        recycler_view_inbox.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        recycler_view_inbox.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        recycler_view_inbox.layoutManager = SmoothLinearLayoutManager(applicationContext)
        recycler_view_inbox.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    applicationContext,
                    6f
                )
            )
        )


        iv_setting.setOnClickListener {
            openPopupMenu(it)
        }
        find_more.setOnClickListener {
            courseExploreClick()
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

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
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
                        ReferralActivity.startReferralActivity(this@InboxActivity)
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
        }
        popupMenu?.show()
    }
        overlay_layout.setOnClickListener {
            overlay_layout.visibility = View.GONE
        }


    override fun showExpiryTimeToolTip() {

    }

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
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
                        ReferralActivity.startReferralActivity(this@InboxActivity)
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
        }
        popupMenu?.show()
    }


    override fun showExpiryTimeToolTip() {
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


    override fun showToolTipBelowFindMoreCourse(remainingTrialDays: Int) {
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


    private fun openSettingActivity() {
        openSettingActivity.launch(SettingsActivity.getIntent(this))
    }

    private fun workInBackground() {
        lifecycleScope.launchWhenCreated {
            processIntent(intent)
            WorkManagerAdmin.determineNPAEvent()
            checkInAppUpdate()
            when {
                NPSEventModel.getCurrentNPA() != null -> {
                    showNetPromoterScoreDialog()
                }
                else -> {
                    if (courseListSet.isNotEmpty()) {
                        locationFetch()
                    }
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handelIntentAction()
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
                                fetchUserLocation()
                            }
                        }
                    }
                }).check()
        }
    }


    private fun addLiveDataObservable() {
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseNetworkData.collect {
                if (it.isNotEmpty())
                    addCourseInRecyclerView(it)
                else {
                    if (isGuestUser()) {
                        openOnBoardFlow()
                    } else {
                        openCourseExplorer()
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseLocalData.collect {
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
                it.showTooltip2,
                courseListSet.size
            )
            setCTAButtonText(exploreType)

            val showOverlay = intent.getBooleanExtra(
                SHOW_OVERLAY,
                false
            ) && it.subscriptionData.isSubscriptionBought.not()
            if (showOverlay && it.showTooltip3 && it.freeTrialData.remainingDays in 4..7) {
                showOverlayToolTip(it.freeTrialData.remainingDays)
            } else {
                overlay_layout.visibility = View.GONE
            }

            if (User.getInstance().isVerified.not() && it?.subscriptionData?.isSubscriptionBought == true) {
                showSignUpDialog()
            }
        }

    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>) {
        if (items.isEmpty()) {
            return
        }
        val temp: ArrayList<InboxEntity> = arrayListOf()
        items.filter { it.isCapsuleCourse }.sortedByDescending { it.courseCreatedDate }.let {
            temp.addAll(it)
        }

        items.filter { (it.created == null || it.created == 0L) && it.courseId != TRIAL_COURSE_ID && it.isCapsuleCourse.not() }
            .sortedByDescending { it.courseCreatedDate }.let {
                temp.addAll(it)
            }

        items.filter { it.created != null && it.created != 0L && it.isCapsuleCourse.not() }
            .sortedByDescending { it.created }.let {
                temp.addAll(it)
            }
        courseListSet.addAll(temp)
        inboxAdapter.addItems(temp)
        if (progress_bar.visibility == View.VISIBLE) {
            progress_bar.visibility = View.GONE
        }
        if (findMoreLayout.visibility == View.INVISIBLE) {
            findMoreLayout.visibility = View.VISIBLE
        }
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
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
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
                    it.is7DFTBought, false
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
                    IS_SUBSCRIPTION_STARTED, it.isSubscriptionBought, false
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

    override fun openCourseExplorer() {
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            courseListSet, state = ActivityEnum.Inbox
        )
    }

    override fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean) {
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
            } else if (resultCode == Activity.RESULT_CANCELED && courseListSet.isNullOrEmpty()) {
                this@InboxActivity.finish()
            }
        } else if (requestCode == USER_DETAILS_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish()
        }
    }


    override fun openCourseExplorer() {
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            courseListSet, state = ActivityEnum.Inbox
        )
    }

    override fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean) {
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
            } else if (resultCode == Activity.RESULT_CANCELED && courseListSet.isNullOrEmpty()) {
                this@InboxActivity.finish()
            }
        } else if (requestCode == USER_DETAILS_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish()
        }
    }


    override fun onUpdateLocation(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            uploadUserLocation(location)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (WebRtcService.isCallWasOnGoing.not()) {
            RtcEngine.destroy()
        }
        inAppUpdateManager = null
        inAppUpdateManager?.onDestroy()
    }
}
