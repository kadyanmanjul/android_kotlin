package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_COURSE_INBOX_TOOLTIP
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.interfaces.OnOpenCourseListener
import com.joshtalks.joshskills.core.notification.StickyNotificationService
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityInboxBinding
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.errorState.BUY_COURSE_FEATURE_ERROR
import com.joshtalks.joshskills.ui.errorState.ErrorActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.adapter.InboxAdapter
import com.joshtalks.joshskills.ui.inbox.adapter.InboxRecommendedCourse
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.FREE_TRIAL_PAYMENT_TEST_ID
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_RECOMMENDED_COURSE
import com.joshtalks.joshskills.util.FileUploadService
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.android.synthetic.main.inbox_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import com.joshtalks.joshskills.voip.data.local.PrefManager as VoipPrefManager

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
    private var isCapsuleCourseBought = false
    var event = EventLiveData

    private val binding by lazy<ActivityInboxBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_inbox)
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissions ->
            if (PrefManager.getBoolValue(NOTIFICATION_POPUP_SHOWN,false).not()) {
                viewModel.saveNotificationPermissionGrantStatus(permissions)
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ).not() && permissions.not()
                ) {
                    PrefManager.put(NOTIFICATION_POPUP_SHOWN, true, false)
                    PermissionUtils.notificationDeniedPermissionDialog(this)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        viewModel.userOnlineStatusSync()
        FileUploadService.uploadAllPendingTasks(AppObjectController.joshApplication)
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        binding.vm = viewModel
        binding.executePendingBindings()
        if (PrefManager.getBoolValue(IS_FREE_TRIAL))
            binding.content.setBackgroundColor(this.resources.getColor(R.color.pure_white))
        initView()
        addLiveDataObservable()
        addAfterTime()
        viewModel.handleGroupTimeTokens()
        viewModel.handleBroadCastEvents()
        MarketingAnalytics.openInboxPage()
        watchTimeEvent()
        event.observe(this) {
            when(it.what){
                CLICK_ON_RECOMMENDED_COURSE -> {
                    val data = it.obj as InboxRecommendedCourse
                    CourseDetailsActivity.startCourseDetailsActivity(
                        this,
                        data.id,
                        startedFrom = this@InboxActivity.javaClass.simpleName,
                        isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT),
                        buySubscription = false
                    )
                }
                BUY_COURSE_FEATURE_ERROR -> {
                    val map = it.obj as HashMap<*, *>
                    openErrorScreen(errorCode = BUY_COURSE_FEATURE_ERROR.toString(), map)
                }
            }
        }
        checkAndGrantNotificationsPermissions()
    }

    private fun checkAndGrantNotificationsPermissions(){
        val isNotificationEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        viewModel.saveNotificationEnabledStatus(isNotificationEnabled)
        if(isNotificationEnabled.not()){
            requestPermissionsLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun openErrorScreen(errorCode:String, map:HashMap<*, *>){
        ErrorActivity.showErrorScreen(
            errorTitle = "Something went wrong",
            errorSubtitle = getString(R.string.error_message_screen),
            errorCode = errorCode,
            activity = this@InboxActivity,
            payload = map["payload"].toString(),
            exception = map["exception"].toString()
        )
    }
    fun watchTimeEvent() {
        try {
            lifecycleScope.launch {
                val videoEngageEntity =
                    AppObjectController.appDatabase.videoEngageDao().getWatchTime()
                videoEngageEntity?.totalTime?.run {
                    val time = TimeUnit.MILLISECONDS.toMinutes(this).div(10).toInt()
                    MarketingAnalytics.logAchievementLevelEvent(time)
                    if (PrefManager.getBoolValue(IS_FREE_TRIAL)){
                        MarketingAnalytics.logAchievementLevelEventFOrFreeTrial(time)
                    }
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
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
            viewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_ICON)

            ReferralActivity.startReferralActivity(this@InboxActivity)
            MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
        }

        btn_upgrade.setOnClickListener {
            BuyPageActivity.startBuyPageActivity(
                this,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                ),
                "INBOX_TOOLBAR_BTN"
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
            BuyPageActivity.startBuyPageActivity(
                this,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                ),
                "INBOX_BUY_COURSE_BTN"
            )
        }
    }

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
                        viewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_MENU)
                        ReferralActivity.startReferralActivity(this@InboxActivity)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_help -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                        openHelpActivity()
                    }
                    R.id.menu_settings -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.SETTINGS).push()
                        openSettingActivity()
                    }
                    R.id.menu_transaction -> {
                        TransactionActivity.startActivity(this@InboxActivity)
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
            checkCouponNotification()
        }
    }

    private fun checkCouponNotification() {
        try {
            if (PrefManager.getStringValue(STICKY_COUPON_DATA).isEmpty()) {
                stopService(Intent(this, StickyNotificationService::class.java))
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notifications: Array<StatusBarNotification> = mNotificationManager.activeNotifications
                for (notification in notifications) {
                    if (notification.id == 10206) {
                        return
                    }
                }
                val json = JSONObject(PrefManager.getStringValue(STICKY_COUPON_DATA))
                val offsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
                val endTime = json.getLong("expiry_time") * 1000L
                if (System.currentTimeMillis().plus(offsetTime) < endTime) {
                    val serviceIntent = Intent(this, StickyNotificationService::class.java)
                    serviceIntent.putExtra("sticky_title", json.getString("sticky_title"))
                    serviceIntent.putExtra("sticky_body", json.getString("sticky_body"))
                    serviceIntent.putExtra("coupon_code", json.getString("coupon_code"))
                    serviceIntent.putExtra("expiry_time", json.getLong("expiry_time") * 1000L)
                    serviceIntent.putExtra("start_from_inbox", true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        startForegroundService(serviceIntent)
                    else
                        startService(serviceIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handleIntentAction()
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
            viewModel.registerCourseLocalData.collect {
                if (it.isNullOrEmpty()) {
                    openCourseExplorer()
                } else {
                    MixPanelTracker.publishEvent(MixPanelEvent.INBOX_OPENED).push()
                    addCourseInRecyclerView(it)
                }
            }
        }
        viewModel.paymentNotInitiated.observe(this) { paymentNotInitiated ->

            if (paymentNotInitiated){
                if (PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT) >= 1){
                    findMoreLayout.visible()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val lastPaymentEntry = AppObjectController.appDatabaseConsistents.branchLogDao().getBranchLogData()
            if (lastPaymentEntry != null && lastPaymentEntry.isSync == 0) {
                AppObjectController.appDatabaseConsistents.branchLogDao().deleteBranchEntry(lastPaymentEntry.orderId)
                MarketingAnalytics.coursePurchased(
                    BigDecimal(lastPaymentEntry.amount),
                    true,
                    testId = lastPaymentEntry.testId,
                    courseName = lastPaymentEntry.courseName,
                    juspayPaymentId = lastPaymentEntry.orderId
                )
            }
        }
        viewModel.paymentStatus.observe(this, Observer {
            when (it.status) {
                PaymentStatus.SUCCESS -> {
                    val freeTrialTestId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                        Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
                    } else {
                        PrefManager.getStringValue(PAID_COURSE_TEST_ID)
                    }
                    if (!PrefManager.getBoolValue(IS_PURCHASE_BRANCH_EVENT_PUSH)){
                        PrefManager.put(IS_PURCHASE_BRANCH_EVENT_PUSH, true)
                        viewModel.saveBranchPaymentLog(it.razorpayOrderId,
                            BigDecimal(it?.amount?:0.0),
                            testId = Integer.parseInt(freeTrialTestId),
                            courseName = "Spoken English Course")
                        MarketingAnalytics.coursePurchased(
                            BigDecimal(it?.amount?:0.0),
                            true,
                            testId = freeTrialTestId,
                            courseName = "Spoken English Course",
                            juspayPaymentId = it.razorpayOrderId
                        )
                    }
                    dismissBbTip()
                    PrefManager.put(IS_APP_RESTARTED, false)
                    initPaymentStatusView(
                        R.drawable.green_rectangle_with_green_stroke,
                        R.drawable.ic_payment_small_tick,
                        R.color.success,
                        R.color.success,
                        R.string.success_payment_text,
                        R.string.success_payment_desc,
                        isTryAgainVisible = false,
                        isHelpLineVisible = false
                    )
                    PrefManager.put(IS_PAYMENT_DONE, true)
                }
                PaymentStatus.FAILED -> {
                    dismissBbTip()
                    initPaymentStatusView(
                        R.drawable.pink_rectangle_with_red_stroke,
                        R.drawable.ic_payment_exclamation,
                        R.color.critical,
                        R.color.critical,
                        R.string.failed_payment_text,
                        R.string.failed_payment_desc,
                        isTryAgainVisible = true,
                        isHelpLineVisible = true
                    )
                }
                PaymentStatus.PROCESSING -> {
                    dismissBbTip()
                    initPaymentStatusView(
                        R.drawable.yellow_rectangle_with_orange_stroke,
                        R.drawable.alert_processing,
                        R.color.accent_600,
                        R.color.accent_600,
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
            if (paymentStatusView.isVisible) {
                val scale = resources.displayMetrics.density
                val dpAsPixels = (100 * scale + 0.5f).toInt()
                inbox_nested_scroll.updatePadding(0, 0, 0, dpAsPixels)
            }
        })
    }

    private fun dismissBbTip(){
        binding.viewArrow.visibility = GONE
        binding.bbTipLayout.visibility = GONE
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
        val tryAgain = paymentStatusView.findViewById<TextView>(R.id.try_again)
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
        if (isTryAgainVisible && !isCapsuleCourseBought) {
            tryAgain.apply {
                visibility = View.VISIBLE
                textColorSet(colorTintIcon)
                backgroundTintList = ContextCompat.getColorStateList(this@InboxActivity, colorTintIcon)
                TextViewCompat.setCompoundDrawableTintList(this, ContextCompat.getColorStateList(this@InboxActivity, colorTintIcon))
                setOnClickListener {
                    BuyPageActivity.startBuyPageActivity(
                        this@InboxActivity,
                        AppObjectController.getFirebaseRemoteConfig().getString(
                            FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                        ),
                        "INBOX_TRY_AGAIN"
                    )
                }
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
                            if(VoipPrefManager.getVoipServiceStatus())
                                LocalBroadcastManager.getInstance(this@InboxActivity).sendBroadcast(broadcastIntent)
                        }
                        if (inboxEntity.isCourseBought && inboxEntity.isCapsuleCourse) {
                            PrefManager.put(IS_COURSE_BOUGHT, true)
                        }

                        if (inboxEntity.isCourseBought.not()) {
                            haveFreeTrialCourse = true
                            PrefManager.put(IS_FREE_TRIAL, true)
                        }
                        if(inboxEntity.isCapsuleCourse && inboxEntity.isCourseBought.not()){
                            viewModel.getBuyPageFeature()
                        }
                    }
                    temp.addAll(courseList)
                    if (courseList.isNullOrEmpty().not()) {
                        val capsuleCourse = courseList[0]
                        PrefManager.put(CURRENT_COURSE_ID, capsuleCourse.courseId)
                        PrefManager.put(PAID_COURSE_TEST_ID, capsuleCourse.paidTestId ?: FREE_TRIAL_PAYMENT_TEST_ID)
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
                PrefManager.put(IS_SUBSCRIPTION_STARTED, isSubscriptionCourseBought)
                isCapsuleCourseBought = capsuleCourse != null && capsuleCourse.isCourseBought
                if (PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT) >= 1) {
                    if (paymentStatusView.visibility != View.VISIBLE) {
//                        lifecycleScope.launch {
//                            delay(1000)
//                            findMoreLayout.visibility = View.VISIBLE
//                            paymentStatusView.visibility = View.GONE
//                        }
                    }
                    Log.e("sagar", "addCourseInRecyclerView: ${isSubscriptionCourseBought }  ${capsuleCourse?.isCapsuleCourse == false} ${capsuleCourse?.isCapsuleCourse}", )
                    if (isSubscriptionCourseBought || (capsuleCourse?.isCapsuleCourse == null || !capsuleCourse.isCapsuleCourse)) {
                        Log.e("sagar", "addCourseInRecyclerView: ${capsuleCourse?.isCapsuleCourse == false}")
                        findMoreLayout.findViewById<MaterialButton>(R.id.find_more).isVisible = true
                        findMoreLayout.findViewById<MaterialButton>(R.id.buy_english_course).isVisible = false
                    } else if (capsuleCourse?.isCapsuleCourse == true && isCapsuleCourseBought.not()) {
                        findMoreLayout.findViewById<MaterialButton>(R.id.buy_english_course).isVisible = true
                        findMoreLayout.findViewById<MaterialButton>(R.id.find_more).isVisible = false
                        try {
                            runOnUiThread {
                                btn_upgrade.isVisible = haveFreeTrialCourse
                                iv_icon_referral.isVisible = haveFreeTrialCourse.not()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        viewModel.paymentNotInitiated.observe(this@InboxActivity) {
                            if (it) {
                                showBuyCourseTooltip(capsuleCourse?.courseId ?: DEFAULT_COURSE_ID)
                            }
                        }
                        findMoreLayout.findViewById<MaterialButton>(R.id.find_more).isVisible = false
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
                if (capsuleCourse?.bbTipText?.isNotBlank() == true) {
                    viewModel.getRecommendedCourse()
                    binding.textExploreCourse.setOnClickListener {
                        viewModel.savePaymentImpressionForCourseExplorePage("CLICKED_EXPLORE_COURSE", EMPTY)
                        openCourseExplorer()
                    }
                }
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
//                findMoreLayout.visibility = View.VISIBLE
                paymentStatusView.visibility = View.GONE
            }
        }

        initABTest()
        viewModel.getRegisterCourses()
        viewModel.getProfileData(Mentor.getInstance().getId())
    }

    fun showBuyCourseTooltip(courseId: String) {
        try {
            val text = AppObjectController.getFirebaseRemoteConfig().getString(
                BUY_COURSE_INBOX_TOOLTIP + courseId
            )
            if (text.isBlank()) return
            binding.bbTipLayout.visibility = VISIBLE
            binding.viewArrow.visibility = VISIBLE
            binding.bbTipLayout.findViewById<MaterialTextView>(R.id.balloon_text).text = text
        } catch (_: Exception) {
        }
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

    override fun onStartTrialTimer(startTimeInMilliSeconds: Long) {
        trialTimerView.visible()
        trialTimerDivider.visible()
        trialTimerView.startTimer(startTimeInMilliSeconds)
    }

    override fun onStopTrialTimer() {
        trialTimerDivider.gone()
        trialTimerView.removeTimer()
    }

    override fun onFreeTrialEnded() {
        trialTimerView.visible()
        trialTimerDivider.visible()
        trialTimerView.endFreeTrial()

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
