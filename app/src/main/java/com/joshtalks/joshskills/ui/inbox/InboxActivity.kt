package com.joshtalks.joshskills.ui.inbox

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_COURSE_INBOX_TOOLTIP
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.interfaces.OnOpenCourseListener
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.CallWithExpertActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.adapter.InboxAdapter
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.referral.ReferralViewModel
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.util.FileUploadService
import com.moengage.core.analytics.MoEAnalyticsHelper
import com.skydoves.balloon.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.android.synthetic.main.inbox_toolbar.*
import kotlinx.android.synthetic.main.inbox_toolbar.iv_icon_referral
import kotlinx.android.synthetic.main.inbox_toolbar.text_message_title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"
const val IS_FROM_NEW_ONBOARDING = "is_from_new_on_boarding_flow"

class InboxActivity : InboxBaseActivity(), LifecycleObserver, OnOpenCourseListener {

    private var popupMenu: PopupMenu? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var findMoreLayout: View
    private lateinit var paymentStatusView: View
    var isPermissionRequired: Boolean = true
    private val courseListSet: MutableSet<InboxEntity> = hashSetOf()
    private val inboxAdapter: InboxAdapter by lazy { InboxAdapter(this, this) }

    var progressDialog: ProgressDialog? = null
    private val refViewModel: ReferralViewModel by lazy {
        ViewModelProvider(this).get(ReferralViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        viewModel.userOnlineStatusSync()
        FileUploadService.uploadAllPendingTasks(AppObjectController.joshApplication)
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        initView()
        addLiveDataObservable()
        addAfterTime()
        //showInAppReview()
        viewModel.handleGroupTimeTokens()
        viewModel.handleBroadCastEvents()
        MarketingAnalytics.openInboxPage()
    }

    private fun initABTest() {
        viewModel.getA2C1CampaignData(CampaignKeys.A2_C1.name)
    }

    private fun addAfterTime() {
        workInBackground()
        handleIntentAction()
        viewModel.getTotalWatchTime()
    }

    private fun initView() {
        showProgressDialog("Please Wait")
        text_message_title.text = getString(R.string.inbox_header)
        iv_reminder.visibility = GONE
        iv_setting.visibility = View.VISIBLE

        iv_icon_referral.setOnClickListener {
            refViewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_ICON)

            ReferralActivity.startReferralActivity(this@InboxActivity)
            MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
        }

        btn_upgrade.setOnClickListener {
            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                this@InboxActivity,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                )
            )
        }

        findMoreLayout = findViewById(R.id.parent_layout)
        paymentStatusView = findViewById(R.id.payment_layout)
        recycler_view_inbox.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(applicationContext).apply {
                isSmoothScrollbarEnabled = true
            }
        }
        recycler_view_inbox.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    applicationContext,
                    6f
                )
            )
        )
        recycler_view_inbox.adapter = inboxAdapter
        iv_setting.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.THREE_DOTS).push()
            openPopupMenu(it)
        }

        find_more.setOnClickListener {
            courseExploreClick()
        }
        buy_english_course.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BUY_ENGLISH_COURSE).push()
            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                this,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                )
            )
        }
    }

//    private fun showInAppReview() {
//        showToast("Call")
//        val manager = FakeReviewManager(applicationContext)
//        manager.requestReviewFlow().addOnCompleteListener { request ->
//            if (request.isSuccessful) {
//                Log.e("sagar", "showInAppReview: ${request.result}")
//                val reviewInfo = request.result
//                manager.launchReviewFlow(this, reviewInfo).addOnCompleteListener { result ->
//                    if (result.isSuccessful) {
//                       showToast("Review Success")
//                    } else {
//                        showToast("Review Failed")
//                    }
//                }
//            }else{
//                showToast(request.exception?.message ?: "")
//            }
//        }
//    }

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
                        refViewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_MENU)
                        ReferralActivity.startReferralActivity(this@InboxActivity)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_help -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
//                        openHelpActivity()
                        CallWithExpertActivity.open(this)
                    }
                    R.id.menu_settings -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.SETTINGS).push()
                        openSettingActivity()
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }
        popupMenu?.show()
    }

    private fun openSettingActivity() {
        openSettingActivity.launch(SettingsActivity.getIntent(this))
    }

    private fun workInBackground() {
        lifecycleScope.launchWhenResumed {
            processIntent(intent)
            checkInAppUpdate()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handleIntentAction()
    }

    private fun initMoEngage() {
        if (!PrefManager.getBoolValue(MOENGAGE_USER_CREATED)) {
            viewModel.initializeMoEngageUser()
            PrefManager.put(MOENGAGE_USER_CREATED, true)
            MoEAnalyticsHelper.setUniqueId(this, Mentor.getInstance().getId())
        }
    }

    fun showProgressDialog(msg: String) {
        progressDialog = ProgressDialog(this, R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()

    private fun addLiveDataObservable() {
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseNetworkData.collect {
                if (it.isNullOrEmpty()) {
                    openCourseExplorer()
                } else {
                    MixPanelTracker.publishEvent(MixPanelEvent.INBOX_OPENED).push()
                    addCourseInRecyclerView(it)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseLocalData.collect {
                if (it.isNotEmpty()) {
                    addCourseInRecyclerView(it)
                }
            }
        }
        viewModel.paymentStatus.observe(this, Observer {
            when (it.status) {
                PaymentStatus.SUCCESS -> {
                    PrefManager.put(IS_APP_RESTARTED, false)
                    initPaymentStatusView(
                        R.drawable.green_rectangle_with_green_stroke,
                        R.drawable.ic_payment_small_tick,
                        R.color.green_payment,
                        R.color.green_payment_text,
                        R.string.success_payment_text,
                        R.string.success_payment_desc,
                        isTryAgainVisible = false,
                        isHelpLineVisible = false
                    )
                    PrefManager.put(IS_PAYMENT_DONE, true)
                }
                PaymentStatus.FAILED -> {
                    initPaymentStatusView(
                        R.drawable.pink_rectangle_with_red_stroke,
                        R.drawable.ic_payment_exclamation,
                        R.color.payment_status_red,
                        R.color.payment_status_red,
                        R.string.failed_payment_text,
                        R.string.failed_payment_desc,
                        isTryAgainVisible = true,
                        isHelpLineVisible = true
                    )
                }
                PaymentStatus.PROCESSING -> {
                    initPaymentStatusView(
                        R.drawable.yellow_rectangle_with_orange_stroke,
                        R.drawable.ic_payment_exclamation,
                        R.color.but_button_color,
                        R.color.but_button_color,
                        R.string.processing_payment_text,
                        R.string.processing_payment_desc,
                        isTryAgainVisible = true,
                        isHelpLineVisible = false
                    )
                }
                else -> {
                    paymentStatusView.visibility = GONE
                    findMoreLayout.visibility = VISIBLE

                }
            }
        })
    }

    private fun initPaymentStatusView(
        bgDrawableId: Int,
        iconDrawable: Int,
        colorTintIcon: Int,
        textColor: Int,
        titleTextID: Int,
        descTextId: Int,
        isTryAgainVisible: Boolean,
        isHelpLineVisible: Boolean
    ) {
        val icon = paymentStatusView.findViewById<AppCompatImageView>(R.id.info_icon)
        val title = paymentStatusView.findViewById<AppCompatTextView>(R.id.title)
        val description = paymentStatusView.findViewById<AppCompatTextView>(R.id.description)
        val tryAgain = paymentStatusView.findViewById<AppCompatTextView>(R.id.try_again)
        val callText = paymentStatusView.findViewById<AppCompatTextView>(R.id.call)
        val number = paymentStatusView.findViewById<AppCompatTextView>(R.id.number)

        paymentStatusView.visibility = VISIBLE
        findMoreLayout.visibility = GONE
        paymentStatusView.background = ContextCompat.getDrawable(
            AppObjectController.joshApplication,
            bgDrawableId
        )

        icon.setImageDrawable(
            ResourcesCompat.getDrawable(
                AppObjectController.joshApplication.resources,
                iconDrawable,
                null
            )
        )
        icon.imageTintList = ContextCompat.getColorStateList(this, colorTintIcon)

        title.setTextColor(ContextCompat.getColor(this, textColor))
        title.text = getString(titleTextID)
        description.text = getString(descTextId)
        if (isTryAgainVisible) {
            tryAgain.visibility = View.VISIBLE
            tryAgain.setOnClickListener {
                FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                    this,
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                    )
                )
            }
        } else {
            tryAgain.visibility = View.GONE
        }
        if (isHelpLineVisible) {
            val helpLine = "+91 8634503202"
            callText.visibility = View.VISIBLE
            number.visibility = View.VISIBLE
            callText.text = getString(R.string.failed_payment_call_text)
            number.text = helpLine
            callText.setOnClickListener {
                Utils.call(this, helpLine)
            }
            number.setOnClickListener {
                Utils.call(this, helpLine)
            }
        } else {
            callText.visibility = View.GONE
            number.visibility = View.GONE
        }
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>) {
        if (items.isEmpty()) {
            return
        }
//        if(!PrefManager.getBoolValue(IS_LOCALE_UPDATED_IN_SETTINGS) && !PrefManager.getBoolValue(
//                IS_LOCALE_UPDATED_IN_INBOX)) {
//            PrefManager.put(IS_LOCALE_UPDATED_IN_INBOX,true)
//            requestWorkerForChangeLanguage(getLangCodeFromCourseId(items[0].courseId), canCreateActivity = false)
//        }
        dismissProgressDialog()
        var haveFreeTrialCourse = false
        lifecycleScope.launch(Dispatchers.Default) {
            val temp: ArrayList<InboxEntity> = arrayListOf()
            var isServiceStarted = false
            items.filter { it.isCapsuleCourse }.sortedByDescending { it.courseCreatedDate }
                .let { courseList ->
                    courseList.forEach { inboxEntity ->
                        // User is Free Trail
                        if (isServiceStarted.not()) {
                            isServiceStarted = true
                            val broadcastIntent = Intent().apply {
                                action = CALLING_SERVICE_ACTION
                                putExtra(SERVICE_BROADCAST_KEY, START_SERVICE)
                            }
                            LocalBroadcastManager.getInstance(this@InboxActivity).sendBroadcast(broadcastIntent)
                        }
                        if (inboxEntity.isCourseBought.not()) {
                            haveFreeTrialCourse = true
                            PrefManager.put(IS_FREE_TRIAL, true)
                        }
                    }
                    temp.addAll(courseList)
                    if (courseList.isNullOrEmpty().not()) {
                        val capsuleCourse = courseList[0]
                        PrefManager.put(CURRENT_COURSE_ID, capsuleCourse.courseId)
                        PrefManager.put(PAID_COURSE_TEST_ID, capsuleCourse.paidTestId ?: "")
                    }
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
            lifecycleScope.launch(Dispatchers.Main) {
                inboxAdapter.addItems(temp)
                val capsuleCourse = temp.firstOrNull { it.isCapsuleCourse }
                val isSubscriptionCourseBought = temp.firstOrNull { it.courseId == SUBSCRIPTION_COURSE_ID } != null
                val isCapsuleCourseBought = capsuleCourse != null && capsuleCourse.isCourseBought
                if (PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT) >= 2) {
                    if (paymentStatusView.visibility != View.VISIBLE) {
                        findMoreLayout.visibility = View.VISIBLE
                        paymentStatusView.visibility = View.GONE
                    }
                    if (isSubscriptionCourseBought) {
                        findMoreLayout.findViewById<MaterialTextView>(R.id.find_more).isVisible = true
                        findMoreLayout.findViewById<MaterialTextView>(R.id.buy_english_course).isVisible = false
                    } else if (isCapsuleCourseBought.not()) {
                        findMoreLayout.findViewById<MaterialTextView>(R.id.buy_english_course).isVisible = true
                        try {
                            runOnUiThread {
                                btn_upgrade.isVisible = haveFreeTrialCourse
                                iv_icon_referral.isVisible = haveFreeTrialCourse.not()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        showBuyCourseTooltip(capsuleCourse?.courseId ?: DEFAULT_COURSE_ID)
                        findMoreLayout.findViewById<MaterialTextView>(R.id.find_more).isVisible = false
                    } else {
                        if (paymentStatusView.visibility != View.VISIBLE) {
                            findMoreLayout.visibility = View.GONE
                            paymentStatusView.visibility = View.GONE
                        }
                    }
                } else {
                    if (paymentStatusView.visibility != View.VISIBLE) {
                        findMoreLayout.visibility = View.GONE
                        paymentStatusView.visibility = View.GONE
                    }
                }
                viewModel.checkForPendingPayments()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PrefManager.put(
            INBOX_SCREEN_VISIT_COUNT,
            PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT).plus(1)
        )
        if (findMoreLayout.visibility != View.VISIBLE &&
            PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT) >= 2
        ) {
            if (paymentStatusView.visibility != View.VISIBLE) {
                findMoreLayout.visibility = View.VISIBLE
                paymentStatusView.visibility = View.GONE
            }
        }

        initABTest()
        initMoEngage()
        viewModel.getRegisterCourses()
        viewModel.getProfileData(Mentor.getInstance().getId())
        if (!PrefManager.getBoolValue(FETCHED_SCHEDULED_NOTIFICATION))
            viewModel.getFreeTrialNotifications()
    }

    fun showBuyCourseTooltip(courseId: String) {
        val text = AppObjectController.getFirebaseRemoteConfig().getString(
            BUY_COURSE_INBOX_TOOLTIP + courseId
        )
        if (text.isBlank()) return
        val balloon = Balloon.Builder(this)
            .setLayout(R.layout.layout_bb_tip)
            .setHeight(BalloonSizeSpec.WRAP)
            .setIsVisibleArrow(true)
            .setBackgroundColorResource(R.color.bb_tooltip_stroke)
            .setArrowDrawable(ContextCompat.getDrawable(this, R.drawable.ic_arrow_yellow_stroke))
            .setWidthRatio(0.85f)
            .setDismissWhenTouchOutside(true)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(this)
            .setDismissWhenClicked(true)
            .setAutoDismissDuration(3000L)
            .setPreferenceName("BUY_COURSE_ENGLISH_TOOLTIP")
            .setShowCounts(3)
            .build()
        val textView = balloon.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
        textView.text = text
        balloon.showAlignBottom(buy_english_course)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
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
        lifecycleScope.launch(Dispatchers.IO) {
            uploadUserLocation(location)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager = null
        inAppUpdateManager?.onDestroy()
    }

    override fun onClick(inboxEntity: InboxEntity) {
        val check = viewModel.abTestRepository.isVariantActive(VariantKeys.EFT_ENABLED)
        if (check && inboxEntity.isFreeTrialExtendable) {
            PrefManager.put(IS_FREE_TRIAL_CAMPAIGN_ACTIVE, true)
            ExtendFreeTrialActivity.startExtendFreeTrialActivity(this, inboxEntity)
        } else {
            ConversationActivity.startConversionActivity(this, inboxEntity)
        }
    }

    companion object {
        fun getInboxIntent(context: Context) = Intent(context, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    override fun onBackPressed() {
        applicationClosed()
        super.onBackPressed()
    }

    private fun applicationClosed() {
        val broadcastIntent = Intent().apply {
            action = CALLING_SERVICE_ACTION
            putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
        }
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }
}
