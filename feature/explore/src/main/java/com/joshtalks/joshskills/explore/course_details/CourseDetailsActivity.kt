package com.joshtalks.joshskills.explore.course_details

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.textview.MaterialTextView
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.base.EventLiveData
import com.joshtalks.joshskills.common.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.common.constants.PAYMENT_PENDING
import com.joshtalks.joshskills.common.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.common.core.abTest.CampaignKeys
import com.joshtalks.joshskills.common.core.abTest.VariantKeys
import com.joshtalks.joshskills.common.core.analytics.*
import com.joshtalks.joshskills.common.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.common.core.notification.client_side.ClientNotificationUtils
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.*
import com.joshtalks.joshskills.common.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.explore.SyllabusData
import com.joshtalks.joshskills.common.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.common.repository.server.onboarding.SubscriptionData
import com.joshtalks.joshskills.common.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.common.ui.payment.PaymentFailedDialogNew
import com.joshtalks.joshskills.common.ui.payment.PaymentInProcessFragment
import com.joshtalks.joshskills.common.ui.payment.PaymentPendingFragment
import com.joshtalks.joshskills.common.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.common.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.common.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.common.ui.special_practice.utils.BACK_PRESSED_ON_GATEWAY
import com.joshtalks.joshskills.common.ui.special_practice.utils.BACK_PRESSED_ON_LOADING
import com.joshtalks.joshskills.common.ui.special_practice.utils.GATEWAY_INITIALISED
import com.joshtalks.joshskills.common.ui.special_practice.utils.PROCEED_PAYMENT_CLICK
import com.joshtalks.joshskills.common.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.common.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.common.util.DividerItemDecoration
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.course_details.adapters.CourseDetailsAdapter
import com.joshtalks.joshskills.explore.course_details.adapters.TeacherDetailsFragment
import com.joshtalks.joshskills.explore.course_details.models.Card
import com.joshtalks.joshskills.explore.course_details.models.CardType
import com.joshtalks.joshskills.explore.course_details.models.TeacherDetails
import com.joshtalks.joshskills.explore.course_details.viewholder.CourseDetailsBaseCell
import com.joshtalks.joshskills.explore.databinding.ActivityCourseDetailsBinding
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import com.joshtalks.skydoves.balloon.OnBalloonClickListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.math.BigDecimal
import org.json.JSONObject

const val ENGLISH_COURSE_TEST_ID = 102
const val ENGLISH_FREE_TRIAL_1D_TEST_ID = 784
const val TRIAL_TEST_ID = 13

class CourseDetailsActivity : ThemedBaseActivity(), OnBalloonClickListener, PaymentGatewayListener {

    private lateinit var binding: ActivityCourseDetailsBinding
    private lateinit var navigator: Navigator
    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var compositeDisposable = CompositeDisposable()
    private var testId: Int = 0
    private var isFromFreeTrial: Boolean = false
    private var isFromNewFreeTrial: Boolean = false
    private var buySubscription: Boolean = false
    private var flowFrom: String? = null
    private var downloadID: Long = -1
    private var is100PointsActive = false
    private var isCoursebought : Boolean = false

    var expiredTime: Long = 0L
    var isPointsScoredMoreThanEqualTo100 = false
    private var shouldStartPayment = false
    private var isPaymentInitiated = false
    private lateinit var bbTooltip: Balloon
    private val backPressMutex = Mutex(false)
    private var event = EventLiveData
    lateinit var dialog: AlertDialog

    private val paymentManager: PaymentManager by lazy {
        PaymentManager(this, viewModel.viewModelScope, this)
    }

    private val appAnalytics by lazy { AppAnalytics.create(AnalyticsEvent.COURSE_OVERVIEW.NAME) }
    private var loginFreeTrial = false
    private var courseName = EMPTY
    private var onDownloadCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                showToast(getString(R.string.downloaded_syllabus))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        // window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.pure_white)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_details)
        binding.lifecycleOwner = this
        binding.handler = this
        testId = intent.getIntExtra(KEY_TEST_ID, 0)
        isFromFreeTrial = intent.getBooleanExtra(IS_FROM_FREE_TRIAL, false)
        buySubscription = intent.getBooleanExtra(BUY_SUBSCRIPTION, false)
        isCoursebought = intent.getBooleanExtra(IS_COURSE_BOUGHT, false)
        if (intent.hasExtra(STARTED_FROM)) {
            flowFrom = intent.getStringExtra(STARTED_FROM)
        }

        navigator = intent.getSerializableExtra(NAVIGATOR) as Navigator
        if (intent.getStringExtra(STARTED_FROM) == "BuyPageActivity") {
            binding.priceContainer.visibility = GONE
            binding.txtExtraHint.visibility = GONE
        } else {
            binding.priceContainer.visibility = View.VISIBLE
        }
        if (testId == ENGLISH_COURSE_TEST_ID || testId == ENGLISH_FREE_TRIAL_1D_TEST_ID) {
            initABTest()
        } else {
            if (testId != 0) {
                getCourseDetails(testId)
            } else {
                finish()
            }
        }

        AppAnalytics.create(AnalyticsEvent.LANDING_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
            .push()
        appAnalytics.addBasicParam()
            .addUserDetails()
            .addParam("test_id", testId)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
        initView()
        val remainingTrialDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS)
        val freeTrialData = FreeTrialData.getMapObject()

        if ((testId == PrefManager.getIntValue(SUBSCRIPTION_TEST_ID) || testId == 122)
            && freeTrialData?.is7DFTBought == true
            && (SubscriptionData.getMapObject()?.isSubscriptionBought == true).not()
            && remainingTrialDays in 0..7
            && PrefManager.getBoolValue(SHOW_COURSE_DETAIL_TOOLTIP)
        ) {
            showTooltip(remainingTrialDays)
        } else {
            if (intent.getStringExtra(STARTED_FROM) == "BuyPageActivity")
                binding.txtExtraHint.visibility = GONE
            else
                binding.txtExtraHint.visibility = View.VISIBLE
//            binding.continueTip.visibility = View.GONE
        }
        subscribeLiveData()
        MarketingAnalytics.openPreCheckoutPage()
        paymentManager.initializePaymentGateway()
    }

    private fun initABTest() {
        viewModel.get100PCampaignData(CampaignKeys.HUNDRED_POINTS.NAME, testId.toString())
    }

    private fun showTooltip(remainingTrialDays: Int) {
        when (remainingTrialDays) {
            5, 6, 7 -> {
                val offerPercentage =
                    AppObjectController.getFirebaseRemoteConfig().getString("COURSE_MAX_OFFER_PER")

                val text = String.format(
                    AppObjectController.getFirebaseRemoteConfig().getString("BUY_COURSE_OFFER_HINT"),
                    offerPercentage,
                    "${PrefManager.getIntValue(REMAINING_TRIAL_DAYS).minus(4)}"
                )
                binding.txtExtraHint.visibility = GONE
            }
            3, 4 -> {
                val text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BUY_COURSE_LAST_DAY_OFFER_HINT)
                binding.txtExtraHint.visibility = GONE
            }
            else -> {
                if (intent.getStringExtra(STARTED_FROM) == "BuyPageActivity")
                    binding.txtExtraHint.visibility = GONE
                else
                    binding.txtExtraHint.visibility = View.VISIBLE
            }
        }
    }

    private fun initView() {
        linearLayoutManager = SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.placeHolderView.setHasFixedSize(true)
        binding.placeHolderView.layoutManager = linearLayoutManager
        binding.placeHolderView.addItemDecoration(
            DividerItemDecoration(this, R.drawable.list_divider)
        )
    }

    private fun visibleBuyButton() {
        if (binding.buyCourseLl.visibility == GONE) {
            binding.buyCourseLl.visibility = View.VISIBLE
        }
    }

    private fun subscribeLiveData() {
        event.observe(this) {
            when (it.what) {
                PAYMENT_SUCCESS -> onPaymentSuccess()
                PAYMENT_FAILED -> showPaymentFailedDialog()
                PAYMENT_PENDING -> showPendingDialog()
            }
        }
        viewModel.courseDetailsLiveData.observe(this) { data ->
            visibleBuyButton()
            if ((data.totalPoints ?: 0) > 100) {
                isPointsScoredMoreThanEqualTo100 = true
            }

            if (is100PointsActive && testId == ENGLISH_COURSE_TEST_ID) {
                expiredTime = data?.expiredDate?.time ?: 0L
                if (isPointsScoredMoreThanEqualTo100 || expiredTime <= System.currentTimeMillis()) {
                    binding.btnStartCourse.isEnabled = true
                    binding.btnStartCourse.alpha = 1f
                } else if (!isPointsScoredMoreThanEqualTo100 && expiredTime > System.currentTimeMillis()) {
                    binding.btnStartCourse.text = getString(R.string.achieve_100_points_to_buy)
                    binding.btnStartCourse.isEnabled = false
                    binding.btnStartCourse.alpha = .5f
                }
            }

            isFromNewFreeTrial = data.isFreeTrial
            binding.txtActualPrice.text = data.paymentData.actualAmount
            binding.txtDiscountedPrice.text = data.paymentData.discountedAmount
            if (data.paymentData.discountText.isNullOrEmpty().not()) {
                binding.txtExtraHint.text = data.paymentData.discountText
                appAnalytics.addParam(
                    AnalyticsEvent.COURSE_PRICE.NAME,
                    data.paymentData.actualAmount
                )
                    .addParam(
                        AnalyticsEvent.SHOWN_COURSE_PRICE.NAME,
                        data.paymentData.discountedAmount
                    )
            } else {
                binding.txtExtraHint.visibility = GONE
            }
            if (data.paymentData.beforeDiscountAmt.isNullOrEmpty().not()) {
                binding.txtDiscountedPrice.setTextColor(Color.parseColor("#107BE5"))
                binding.txtBeforeDiscountAmt.text = data.paymentData.beforeDiscountAmt
                binding.txtBeforeDiscountAmt.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            }
            if (data.paymentData.bbTipText.isNullOrEmpty().not()) {
                showBbTooltip(
                    data.paymentData.bbTipText!!,
                    data.paymentData.discountedAmount.substring(1).toDouble() != 0.0
                )
            }
            if (data.paymentData.encryptedText.isNullOrEmpty().not()) {
                shouldStartPayment = true
            }
            if (data.version.isNotBlank()) {
                appAnalytics.addParam(VERSION, PrefManager.getStringValue(VERSION))
                PrefManager.put(VERSION, data.version)
            }
            binding.txtActualPrice.paintFlags = binding.txtActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            binding.placeHolderView.adapter = CourseDetailsAdapter(
                this,
                testId,
                data.cards.sortedBy { it.sequenceNumber }.plusElement(
                    Card(-1, CardType.OTHER_INFO, JsonObject())
                )
            )

            updateButtonText(data.paymentData.discountedAmount.substring(1).toDouble())

        }

        viewModel.apiCallStatusLiveData.observe(this) {
            binding.progressBar.visibility = GONE
            if (it == ApiCallStatus.FAILED) {
                val imageUrl = AppObjectController.getFirebaseRemoteConfig().getString("ERROR_API_IMAGE_URL")
                val imageView = ImageView(this).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                        CoordinatorLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                }

                binding.coordinator.addView(imageView)
                Glide.with(this)
                    .load(imageUrl)
                    .override(Target.SIZE_ORIGINAL)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    ).into(imageView)
            } else if (it == ApiCallStatus.START) {
                showMoveToInboxScreen()
            }
        }

        viewModel.points100ABtestLiveData.observe(this) { abTestCampaignData ->
            abTestCampaignData?.let { map ->
                is100PointsActive =
                    (map.variantKey == VariantKeys.POINTS_HUNDRED_ENABLED.NAME) && map.variableMap?.isEnabled == true
            }
        }
    }

    private fun showBbTooltip(bbTipText: String, shouldShow: Boolean) {
        try {
            if (shouldShow && (intent.getStringExtra(STARTED_FROM) != "BuyPageActivity")) {
                bbTooltip = Balloon.Builder(this)
                    .setLayout(R.layout.layout_bb_tip)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setIsVisibleArrow(true)
                    .setBackgroundColorResource(R.color.surface_tip)
                    .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                    .setWidthRatio(0.85f)
                    .setDismissWhenTouchOutside(false)
                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                    .setLifecycleOwner(this)
                    .setDismissWhenClicked(false)
                    .build()

                bbTooltip.getContentView().findViewById<MaterialTextView>(R.id.balloon_text).text =
                    bbTipText.replace("__username__", Mentor.getInstance().getUser()?.firstName ?: "User")
                bbTooltip.isShowing.not().let {
                    bbTooltip.showAlignBottom(binding.buyCourseLl)
                }
                val scale = resources.displayMetrics.density
                val dpAsPixels = (110 * scale + 0.5f).toInt()
                binding.placeHolderView.updatePadding(0, 0, 0, dpAsPixels)
            }
        } catch (_: Exception) {
        }
    }

    private fun showMoveToInboxScreen() {
        startActivity(this.getInboxActivityIntent())
        this.finish()
    }

    private fun getCourseDetails(testId: Int) {
        viewModel.fetchCourseDetails(testId.toString())
    }

    private fun scrollToPosition(pos: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: CourseDetailsBaseCell
                // TODO : Correct this
//                binding.placeHolderView.allViewResolvers.let {
//                    it.forEachIndexed { index, view ->
//                        if (view is CourseDetailsBaseCell) {
//                            tempView = view
//                            if (tempView.sequenceNumber == pos) {
//                                AppObjectController.uiHandler.post {
//                                    linearLayoutManager.scrollToPositionWithOffset(index, 0)
//                                }
//                            }
//                        }
//                    }
//                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun scrollToCard(type: CardType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: CourseDetailsBaseCell
                // TODO : Correct this
//                binding.placeHolderView.allViewResolvers.let {
//                    it.forEachIndexed { index, view ->
//                        if (view is CourseDetailsBaseCell) {
//                            tempView = view
//                            if (tempView.type == type) {
//                                AppObjectController.uiHandler.post {
//                                    linearLayoutManager.scrollToPositionWithOffset(index, 0)
//                                }
//                            }
//                        }
//                    }
//                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addObserver() {
        compositeDisposable.add(com.joshtalks.joshskills.common.messaging.RxBus2.listen(TeacherDetails::class.java).subscribe {
            logMeetMeAnalyticEvent(it.name)
            TeacherDetailsFragment.newInstance(it)
                .show(supportFragmentManager, "Teacher Details")
        })
        compositeDisposable.add(
            RxBus2.listen(DownloadSyllabusEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.syllabusData.syllabusDownloadUrl.isBlank().not()) {
                        logDownloadFileAnalyticEvent()
                        getPermissionAndDownloadSyllabus(it.syllabusData)
                    }
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(OfferCardEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    AppObjectController.navigator.with(this).navigate(
                        object : CourseDetailContract {
                            override val testId = 10
                            override val flowFrom = this@CourseDetailsActivity.javaClass.simpleName
                            override val navigator = AppObjectController.navigator
                            override val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT)
                        }
                    )
                    finish()
                },{
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(CardType::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    scrollToCard(it)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Utils.fileUrl(it.localPath, it.serverPath)?.run {
                            ImageShowFragment.newInstance(this, null, null)
                                .show(supportFragmentManager, "ImageShow")
                        }
                    },
                    {
                        it.printStackTrace()
                    })
        )
        compositeDisposable.add(
            RxBus2.listen(VideoShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    MixPanelTracker.publishEvent(MixPanelEvent.COURSE_PLAY_DEMO)
                        .addParam(ParamKeys.TEST_ID,testId)
                        .addParam(ParamKeys.COURSE_NAME,courseName)
                        .addParam(ParamKeys.COURSE_PRICE,viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount)
                        .addParam(ParamKeys.COURSE_ID,PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                        .push()

                    AppAnalytics.create(AnalyticsEvent.DEMO_VIDEO_PLAYED.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(VERSION, PrefManager.getStringValue(VERSION))
                        .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
                    VideoPlayerActivity.startVideoActivity(
                        this,
                        it.videoTitle,
                        it.videoId,
                        it.videoUrl
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(RxBus2.listen(EmptyEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .subscribe {
                fetchUserLocation()
            })
    }

    override fun onUpdateLocation(location: Location) {
        refreshLocationViewHolder(location)
    }

    override fun onDenyLocation() {
        refreshLocationViewHolder(null)
    }

    private fun refreshLocationViewHolder(location: Location?) {
        //TODO : Correct this
//        binding.placeHolderView.allViewResolvers?.let {
//            it.forEachIndexed { index, view ->
//                if (view is LocationStatViewHolder) {
//                    view.location = location
//                    AppObjectController.uiHandler.postDelayed({
//                        binding.placeHolderView.refreshView(index)
//                    }, 250)
//                    return@let
//                }
//            }
//        }
    }

    fun buyCourse() {
        if (shouldStartPayment) {
            viewModel.savePaymentImpressionForCourseExplorePage("PRESSED_BUY_NOW", testId.toString())
            isPaymentInitiated = true
            dismissBbTip()
            paymentManager.createOrder(
                testId.toString(),
                Mentor.getInstance().getUser()?.phoneNumber ?: "+919999999999",
                viewModel.getEncryptedText()
            )
            return
        } else if (buySubscription) {
            val tempTestId = PrefManager.getIntValue(SUBSCRIPTION_TEST_ID)
            logStartCourseAnalyticEvent(tempTestId)
            PaymentSummaryActivity.startPaymentSummaryActivity(
                this,
                testId = PrefManager.getIntValue(SUBSCRIPTION_TEST_ID).toString(),
                isFromNewFreeTrial = isFromNewFreeTrial
            )
        } else if (isFromFreeTrial) {
            val isTrialEnded = PrefManager.getBoolValue(IS_TRIAL_ENDED, false)
            if (isTrialEnded || testId == PrefManager.getIntValue(SUBSCRIPTION_TEST_ID)
            ) {
                val tempTestId = PrefManager.getIntValue(SUBSCRIPTION_TEST_ID)
                logStartCourseAnalyticEvent(tempTestId)
                PaymentSummaryActivity.startPaymentSummaryActivity(
                    this,
                    tempTestId.toString(),
                    isFromNewFreeTrial = isFromNewFreeTrial
                )
            } else viewModel.addMoreCourseToFreeTrial(testId)
        } else {
            val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, false)
            val discountedPrice =
                viewModel.courseDetailsLiveData.value!!.paymentData.discountedAmount.substring(1)
                    .toDouble()
            if (exploreTypeStr.isNotBlank()
                && exploreTypeStr == ExploreCardType.FREETRIAL.name
            ) {
                val isTrialStarted = PrefManager.getBoolValue(IS_TRIAL_STARTED, false)
                val isSubscriptionStarted =
                    PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED, false)
                val tempTestId = if (isTrialStarted && discountedPrice > 0.0) {
                    PrefManager.getIntValue(SUBSCRIPTION_TEST_ID)
                } else if (isTrialStarted.not() && isSubscriptionStarted.not()) TRIAL_TEST_ID
                else testId
                logStartCourseAnalyticEvent(tempTestId)
                PaymentSummaryActivity.startPaymentSummaryActivity(
                    this,
                    tempTestId.toString(),
                    isFromNewFreeTrial = isFromNewFreeTrial
                )
            } else {
                if(isFromNewFreeTrial) {
                    loginFreeTrial = true
                    MixPanelTracker.publishEvent(MixPanelEvent.LOGIN_FREE_TRIAL_CLICKED).push()
                }
                logStartCourseAnalyticEvent(testId)
                PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString(),isFromNewFreeTrial = isFromNewFreeTrial, is100PointsObtained = isPointsScoredMoreThanEqualTo100 && testId == ENGLISH_COURSE_TEST_ID && is100PointsActive, isHundredPointsActive = is100PointsActive)
            }
            appAnalytics.addParam(AnalyticsEvent.START_COURSE_NOW.NAME, "Clicked")
        }
    }

    private fun dismissBbTip() {
        try {
            if (this::bbTooltip.isInitialized && bbTooltip.isShowing) {
                bbTooltip.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logStartCourseAnalyticEvent(testId: Int) {
        if(!loginFreeTrial){
            MixPanelTracker.publishEvent(MixPanelEvent.COURSE_START_NOW)
                .addParam(ParamKeys.TEST_ID,testId)
                .addParam(ParamKeys.COURSE_NAME,courseName)
                .addParam(ParamKeys.COURSE_PRICE,viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount)
                .push()
        }
        AppAnalytics.create(AnalyticsEvent.START_COURSE_NOW.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
    }

    private fun logMeetMeAnalyticEvent(name: String) {
        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_MEET_INSTRUCTOR)
            .addParam(ParamKeys.TEST_ID,testId)
            .addParam(ParamKeys.COURSE_NAME,courseName)
            .addParam(ParamKeys.INSTRUCTOR_NAME,name)
            .addParam(ParamKeys.COURSE_PRICE,viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount)
            .addParam(ParamKeys.COURSE_ID,PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
            .push()

        AppAnalytics.create(AnalyticsEvent.MEET_ME_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .addParam("Name", name)
            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
    }

    private fun logDownloadFileAnalyticEvent() {
        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_DOWNLOAD_SYLLABUS)
            .addParam(ParamKeys.TEST_ID,testId)
            .addParam(ParamKeys.COURSE_NAME,courseName)
            .addParam(ParamKeys.COURSE_PRICE,viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount)
            .addParam(ParamKeys.COURSE_ID,PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
            .push()

        AppAnalytics.create(AnalyticsEvent.DOWNLOAD_FILE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }

    private fun getPermissionAndDownloadSyllabus(syllabusData: SyllabusData) {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            downloadDigitalCopy(syllabusData)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(this@CourseDetailsActivity)
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    private fun downloadDigitalCopy(syllabusData: SyllabusData) {
        registerDownloadReceiver()
        var fileName = Utils.getFileNameFromURL(syllabusData.syllabusDownloadUrl)
        if (fileName.isEmpty()) {
            syllabusData.title.run {
                fileName = this + "_syllabus.pdf"
            }
        }
        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(syllabusData.syllabusDownloadUrl))
                .setTitle(getString(R.string.app_name))
                .setDescription("Downloading syllabus")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
        }

        val downloadManager =
            AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)
        downloadID = downloadManager.enqueue(request)
        showToast(getString(R.string.downloading_start))
    }

    private fun registerDownloadReceiver() {
        AppObjectController.joshApplication.registerReceiver(
            onDownloadCompleteListener,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onDestroy() {
        try {
            this.unregisterReceiver(onDownloadCompleteListener)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }

    fun goToTop() {
        val params: CoordinatorLayout.LayoutParams =
            binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior
        behavior.onNestedFling(
            binding.coordinator,
            binding.appBarLayout,
            binding.coordinator,
            0f,
            10000f,
            true
        )
    }

    private fun updateButtonText(discountedPrice: Double) {
        if (discountedPrice == 0.0) {
            shouldStartPayment = false
            binding.txtExtraHint.visibility = GONE
            binding.btnStartCourse.text = getString(R.string.start_free_course)
            binding.btnStartCourse.textSize = 16f
            dismissBbTip()
        }

        val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, false)
        if (exploreTypeStr.isNotBlank()) {
            when (ExploreCardType.valueOf(exploreTypeStr)) {
                ExploreCardType.FREETRIAL -> {
                    if (discountedPrice > 0) {
                        binding.btnStartCourse.text = getString(R.string.get_one_year_pass)
                        binding.btnStartCourse.textSize = 16f
                    }
                }
                else -> {}
            }
        }

        if (viewModel.courseDetailsLiveData.value?.isFreeTrial?:false || isFromNewFreeTrial ) {
            binding.btnStartCourse.text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString("${FirebaseRemoteConfigKey.FREE_TRIAL_COURSE_DETAIL_BTN_TXT}_$testId")
        }
    }

    companion object {
        const val KEY_TEST_ID = "test-id"
        const val WHATSAPP_URL = "whatsapp-url"
        const val IS_FROM_FREE_TRIAL = "is_from_free_trial"
        const val BUY_SUBSCRIPTION = "buy_subscription"
        const val IS_COURSE_BOUGHT = "is_course_bought"

        fun openCourseDetailsActivity(contract: CourseDetailContract, context: Context) {
            context.startActivity(
                Intent(context, CourseDetailsActivity::class.java).apply {
                    putExtra(KEY_TEST_ID, contract.testId)
                    putExtra(IS_FROM_FREE_TRIAL, contract.isFromFreeTrial)
                    putExtra(BUY_SUBSCRIPTION, contract.buySubscription)
                    putExtra(IS_COURSE_BOUGHT, PrefManager.getBoolValue(IS_COURSE_BOUGHT))
                    putExtra(NAVIGATOR, contract.navigator)
                    putExtra(STARTED_FROM, contract.flowFrom)
                    if (contract.whatsappUrl.isNullOrBlank().not()) {
                        putExtra(WHATSAPP_URL, contract.whatsappUrl)
                    }
                    contract.flags.forEach { addFlags(it) }
                }
            )
        }

        fun getIntent(
            context: Context,
            testId: Int,
            startedFrom: String = EMPTY, flags: Array<Int> = arrayOf()
        ) = Intent(context, CourseDetailsActivity::class.java).apply {
            putExtra(KEY_TEST_ID, testId)
            if (startedFrom.isNotBlank())
                putExtra(STARTED_FROM, startedFrom)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }

    private fun showPendingDialog() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentPendingFragment()
            replace(R.id.details_parent_container, fragment, "Payment Pending")
            disallowAddToBackStack()
        }
    }

    override fun onBalloonClick(view: View) {}

    override fun onBackPressed() {
        if (viewModel.getCoursePrice() == 0.0 || intent.getStringExtra(STARTED_FROM) == "BuyPageActivity")
            super.onBackPressed()
        else if (!isPaymentInitiated)
            showBackPressDialog()
        else {
            backPressMutex.onMultipleBackPress {
                val backPressHandled = paymentManager.getJuspayBackPress()
                if (!backPressHandled) {
                    viewModel.savePaymentImpression(BACK_PRESSED_ON_GATEWAY, "COURSE_DETAILS")
                    super.onBackPressed()
                } else {
                    viewModel.savePaymentImpression(BACK_PRESSED_ON_LOADING, "COURSE_DETAILS")
                }
            }
        }
    }

    private fun showBackPressDialog() {
        if (::dialog.isInitialized)
            dialog.dismiss()
        Log.d("sagar", "showBackPressDialog: ${PrefManager.getStringValue(CURRENT_COURSE_ID)}")
        val builder = AlertDialog.Builder(this).apply {
            setTitle("${Mentor.getInstance().getUser()?.firstName ?: "User"}, are you sure that you don't want this course?")
            setMessage(AppObjectController.getFirebaseRemoteConfig().getString(FirebaseRemoteConfigKey.GIFT_COURSE_TEXT.plus(
                (testId.toString()).ifEmpty { ENGLISH_FREE_TRIAL_1D_TEST_ID })
            ))
            setCancelable(false)
            setPositiveButton("Buy Now") { p0, p1 ->
                buyCourse()
            }
            setNegativeButton("Later") { p0, p1 ->
                super.onBackPressed()
            }
        }
        dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
        }
        dialog.show()
    }

    private fun navigateToStartCourseActivity(hasOrderId: Boolean) {
        StartCourseActivity.openStartCourseActivity(
            this,
            viewModel.getCourseName(),
            viewModel.getTeacherName(),
            viewModel.getImageUrl(),
            if (hasOrderId)
                paymentManager.getJoshTalksId()
            else 0,
            testId.toString(),
            viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount.toString()
        )
        this@CourseDetailsActivity.finish()
    }

    private fun logPaymentStatusAnalyticsEvents(status: String, reason: String? = "Completed") {
        AppAnalytics.create(AnalyticsEvent.PAYMENT_STATUS_NEW.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.PAYMENT_STATUS.NAME, status)
            .addParam(AnalyticsEvent.REASON.NAME, reason)
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, viewModel.getCourseName())
            .addParam(AnalyticsEvent.SHOWN_COURSE_PRICE.NAME, viewModel.courseDetailsLiveData.value?.paymentData?.discountedAmount)
            .addParam("test_id", testId)
            .addParam(AnalyticsEvent.COURSE_PRICE.NAME, viewModel.courseDetailsLiveData.value?.paymentData?.actualAmount).push()
    }

    private fun onPaymentSuccess() {
        appAnalytics.addParam(AnalyticsEvent.PAYMENT_COMPLETED.NAME, true)
        logPaymentStatusAnalyticsEvents(AnalyticsEvent.SUCCESS_PARAM.NAME)
        viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        ClientNotificationUtils(applicationContext).removeAllScheduledNotification()
        viewModel.saveBranchPaymentLog(
            paymentManager.getJustPayOrderId(),
            BigDecimal(paymentManager.getAmount()),
            testId = testId,
            courseName = viewModel.getCourseName()
        )
        MarketingAnalytics.coursePurchased(
            BigDecimal(paymentManager.getAmount()),
            true,
            testId = testId.toString(),
            courseName = viewModel.getCourseName(),
            juspayPaymentId = paymentManager.getJustPayOrderId()
        )

        uiHandler.post {
            PrefManager.put(IS_PAYMENT_DONE, true)
        }

        uiHandler.postDelayed({
            navigateToStartCourseActivity(true)
        }, 1000 * 2L)
    }

    private fun showPaymentFailedDialog() {
        try {
            viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.details_parent_container,
                PaymentFailedDialogNew.newInstance(paymentManager),
                "Payment Failed"
            )
        }
    }

    override fun onWarmUpEnded(error: String?) {}

    override fun onProcessStart() {
        viewModel.savePaymentImpression(PROCEED_PAYMENT_CLICK, "COURSE_DETAILS")
        showProgressBar()
    }

    override fun onProcessStop() {
        viewModel.savePaymentImpression(GATEWAY_INITIALISED, "COURSE_DETAILS")
        hideProgressBar()
    }

    override fun onPaymentProcessing(orderId: String, status: String) {
        if (status == "pending_vbv") {
            showPendingDialog()
            return
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentInProcessFragment()
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderId)
            }
            fragment.arguments = bundle
            replace(R.id.details_parent_container, fragment, "Payment Processing")
            disallowAddToBackStack()
        }
    }

    override fun onEvent(data: JSONObject?) {
        data?.let {
            viewModel.logPaymentEvent(data)
        }
    }
}
