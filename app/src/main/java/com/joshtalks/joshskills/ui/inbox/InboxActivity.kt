package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
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
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.SubscriptionData
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.joshtalks.skydoves.balloon.Balloon
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.rxlocation.RxLocation
import io.reactivex.CompletableObserver
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.nested_scroll_view
import kotlinx.android.synthetic.main.activity_inbox.progress_bar
import kotlinx.android.synthetic.main.activity_inbox.recycler_view_inbox
import kotlinx.android.synthetic.main.activity_inbox.subscriptionTipContainer
import kotlinx.android.synthetic.main.activity_inbox.txtConvert
import kotlinx.android.synthetic.main.activity_inbox.txtSubscriptionTip
import kotlinx.android.synthetic.main.find_more_layout.find_more
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.Calendar

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_NEW = 2008
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"

class InboxActivity : CoreJoshActivity(), LifecycleObserver, InAppUpdateManager.InAppUpdateHandler {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()
    private var inAppUpdateManager: InAppUpdateManager? = null
    private lateinit var reminderIv: ImageView
    private lateinit var findMoreLayout: View
    private var offerInHint: Balloon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        setToolbar()
        viewModel.updateSubscriptionStatus()
        addLiveDataObservable()
        checkAppUpdate()
        workInBackground()
        handelIntentAction()
    }

    private fun updateSubscriptionTipView(exploreType: ExploreCardType) {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED).not()) {
            when (exploreType) {
                ExploreCardType.FREETRIAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE

                    val remainingTrialDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS)
                    txtSubscriptionTip.text = when {

                        remainingTrialDays <= 0 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)

                        remainingTrialDays == 1 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)

                        remainingTrialDays == 2 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)

                        remainingTrialDays == 3 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)

                        remainingTrialDays == 4 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)

                        remainingTrialDays == 5 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)

                        remainingTrialDays == 6 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)

                        remainingTrialDays > 6 -> AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)

                        else -> EMPTY
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
                    }
                }

                ExploreCardType.NORMAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE
                    viewModel.registerCourseNetworkLiveData.value?.let {
                        txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
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

    private fun setToolbar() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.inbox_header)
        findMoreLayout = findViewById(R.id.parent_layout)
        if (isGuestUser()) {
            getVersionData()?.let {
                when (it.version!!.name) {
                    ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                        find_more.setOnClickListener {
                            AppAnalytics.create(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .push()
                            RxBus2.publish(ExploreCourseEventBus())
                        }
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V3 -> {
                        find_more.text = getString(R.string.add_more_courses)
                        find_more.setOnClickListener {
                            AppAnalytics.create(AnalyticsEvent.ADD_MORE_COURSE_CLICKED.NAME)
                                .addBasicParam()
                                .addUserDetails()
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
        val popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
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
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }

    private fun workInBackground() {
        CoroutineScope(Dispatchers.Default).launch {
            processIntent(intent)
            WorkManagerAdmin.determineNPAEvent()
        }
        when {
            shouldRequireCustomPermission() -> {
                checkForOemNotifications()
            }
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
        val currentAppVersion = BuildConfig.VERSION_CODE
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
    }

    private fun setupForFindMoreCourseButton() {
        CoroutineScope(Dispatchers.Default).launch {
            val scrollBounds = Rect()
            nested_scroll_view.getHitRect(scrollBounds)
            isFindMoreButtonVisible(scrollBounds)
            nested_scroll_view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                isFindMoreButtonVisible(scrollBounds)
            })
        }
    }

    private fun isFindMoreButtonVisible(scrollBounds: Rect) {
        if (findMoreLayout.getLocalVisibleRect(scrollBounds)) {
            if (!findMoreLayout.getLocalVisibleRect(scrollBounds)
                || scrollBounds.height() < findMoreLayout.height
            ) {
                Log.e("inbox", "BTN APPEAR PARCIALY");
                attachOfferHintView()
            } else {
                attachOfferHintView()
                Log.e("inbox", "BTN APPEAR FULLY!!!");
            }
        } else {
            offerInHint?.dismiss()
            Log.e("inbox", "No");
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
            updateSubscriptionTipView(exploreType)
            setCTAButtonText(exploreType)

        }

        viewModel.registerCourseMinimalLiveData.observe(this) {
            addCourseInRecyclerView(it)
        }

        txtConvert.setOnClickListener {
            logEvent(AnalyticsEvent.CONVERT_CLICKED.name)
            PaymentSummaryActivity.startPaymentSummaryActivity(
                this, AppObjectController.getFirebaseRemoteConfig()
                    .getDouble(FirebaseRemoteConfigKey.SUBSCRIPTION_TEST_ID).toInt().toString()
            )
        }

        viewModel.reminderApiCallStatusLiveData.observe(this, {
            if (it.equals(ApiCallStatus.SUCCESS)) {
                animateBell()
            }
        })

        viewModel.totalRemindersLiveData.observe(this) {
            openReminderCallback(it)
        }
    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>?) {
        if (items.isNullOrEmpty()) {
            return
        }
        recycler_view_inbox.removeAllViews()
        val total = items.size
        val newCourses = items.filter { it.created == null || it.created == 0L }
        newCourses.sortedByDescending { it.courseCreatedDate }.forEachWithIndex { index, inbox ->
            if (inbox.courseId != TRIAL_COURSE_ID)
                recycler_view_inbox.addView(
                    InboxViewHolder(
                        inbox, total, index
                    )
                )
        }
        items.filter { it.created != null && it.created != 0L }
            .sortedByDescending { it.created }
            .forEachWithIndex { index, inbox ->
                if (inbox.courseId != TRIAL_COURSE_ID)
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox, total, newCourses.size + index
                        )
                    )
            }
        progress_bar.visibility = View.GONE
        findMoreLayout.visibility = View.VISIBLE
        setupForFindMoreCourseButton()
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

    }

    private fun openNewOnBoardFlow() {
        getVersionData()?.let {
            when (it.version?.name) {
                ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                    openCourseExplorer()
                }
                ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4 -> {
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

    private fun openCourseSelectionExplorer(boolean: Boolean = false) {
        OnBoardingActivityNew.startOnBoardingActivity(this, COURSE_EXPLORER_NEW, true, boolean)
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


    private fun attachOfferHintView() {
        compositeDisposable.add(
            AppObjectController.appDatabase
                .courseDao()
                .isUserInOfferDays()
                .concatMap {
                    val (flag, remainDay) = Utils.isUserInDaysOld(it.courseCreatedDate)
                    if (offerInHint == null) {
                        getVersionData()?.let {
                            when (it.version?.name) {
                                ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                                    offerInHint =
                                        BalloonFactory.offerIn7Days(
                                            this,
                                            this,
                                            remainDay.toString()
                                        )
                                }
                                else -> {
                                    offerInHint =
                                        BalloonFactory.offerIn7Days(
                                            this,
                                            this,
                                            tipText = getVersionData()?.tooltipText!!
                                        )
                                }
                            }
                        }
                        hideToolTip()
                    }
                    return@concatMap Maybe.just(flag)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { value ->
                        hideToolTip(value)
                    },
                    { error ->
                        error.printStackTrace()
                    }
                ))
    }

    private fun hideToolTip(value: Boolean = true) {
        val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, false)
        if (exploreType.isBlank() || exploreType.contentEquals(ExploreCardType.NORMAL.name)) {
            val root = findViewById<View>(R.id.find_more)
            offerInHint?.run {
                if (this.isShowing.not() && isFinishing.not() && value) {
                    this.showAlignBottom(root)
                    findViewById<View>(R.id.bottom_line).visibility = View.GONE
                }
            }
        }
    }

    private fun setTrialEndParam() {
        val freeTrialData = FreeTrialData.getMapObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

    private fun addTopHintViewForTrialEnd() {

    }

}
