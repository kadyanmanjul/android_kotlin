package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.premium.constants.PAYMENT_PENDING
import com.joshtalks.joshskills.premium.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.FirebaseRemoteConfigKey.Companion.BUY_PAGE_SUPPORT_PHONE_NUMBER
import com.joshtalks.joshskills.premium.core.abTest.VariantKeys
import com.joshtalks.joshskills.premium.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.premium.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.premium.core.custom_ui.JoshRatingBar
import com.joshtalks.joshskills.premium.core.interfaces.OnOpenCourseListener
import com.joshtalks.joshskills.premium.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.premium.core.notification.NotificationCategory
import com.joshtalks.joshskills.premium.core.notification.NotificationUtils
import com.joshtalks.joshskills.premium.core.notification.StickyNotificationService
import com.joshtalks.joshskills.premium.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.premium.databinding.ActivityBuyPageBinding
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.ui.assessment.view.Stub
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.premium.ui.errorState.*
import com.joshtalks.joshskills.premium.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.premium.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.premium.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.premium.ui.payment.PaymentFailedDialogNew
import com.joshtalks.joshskills.premium.ui.payment.PaymentInProcessFragment
import com.joshtalks.joshskills.premium.ui.payment.PaymentPendingFragment
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.adapter.BuyPageViewPager
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.fragment.BookACallFragment
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.fragment.CouponCardFragment
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.fragment.RatingAndReviewFragment
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.BuyCourseFeatureModelNew
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.TestimonialVideo
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.premium.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.premium.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.premium.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.premium.ui.special_practice.utils.*
import com.joshtalks.joshskills.premium.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.premium.ui.termsandconditions.WebViewFragment
import com.joshtalks.joshskills.premium.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.premium.util.showAppropriateMsg
import com.joshtalks.joshskills.premium.calling.Utils.Companion.onMultipleBackPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*


const val FREE_TRIAL_PAYMENT_TEST_ID = "102"
const val SUBSCRIPTION_TEST_ID = "10"
const val IS_FAKE_CALL = "is_fake_call"
const val COUPON_APPLY_POP_UP_SHOW_AND_BACK = 1000
const val NORMAL_BACK_PRESS = 1111

class BuyPageActivity : ThemedBaseActivityV2(), PaymentGatewayListener, OnOpenCourseListener {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    var englishCourseCard: View? = null
    var teacherRatingAndReviewCard: View? = null
    var proceedButtonCard: View? = null
    var clickRatingOpen: ImageView? = null
    var courseDescListCard: View? = null
    var priceForPaymentProceed: CourseDetailsList? = null
    var isPaymentInitiated = false
    var couponCodeFromIntent: String? = null
    var shouldAutoApplyCoupon: Boolean = false
    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    private var flowFrom: String = EMPTY
    var paymentButtonValue = 0

    private var countdownTimerBack: CountdownTimerBack? = null
    private var openCourseListener: OnOpenCourseListener? = null
    private val binding by lazy<ActivityBuyPageBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_buy_page)
    }

    private var errorView: Stub<ErrorView>? = null
    private val viewModel by lazy {
        ViewModelProvider(this)[BuyPageViewModel::class.java]
    }
    private val backPressMutex = Mutex(false)
    var isCallUsButtonActive = 0
    val adapter  = BuyPageViewPager()
    private val paymentManager: PaymentManager by lazy {
        PaymentManager(
            this,
            viewModel.viewModelScope,
            this
        )
    }

    private var dotsCount = 0
    private lateinit var dots: Array<ImageView?>

    override fun getArguments() {
        super.getArguments()
        if (intent.hasExtra(HAS_NOTIFICATION)) {
            flowFrom = "NOTIFICATION"
            if (!PrefManager.getBoolValue(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                    IS_COURSE_BOUGHT
                ))
                finish()
        }
        testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
            Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
        } else {
            PrefManager.getStringValue(PAID_COURSE_TEST_ID, defaultValue = FREE_TRIAL_PAYMENT_TEST_ID)
        }
        flowFrom = intent.getStringExtra(FLOW_FROM) ?: EMPTY
        couponCodeFromIntent = intent.getStringExtra(COUPON_CODE)
        shouldAutoApplyCoupon = intent.getBooleanExtra(SHOULD_AUTO_APPLY_COUPON_ARG, false)
        if (intent.hasExtra(IS_FAKE_CALL)) {
            val nameArr = User.getInstance().firstName?.split(" ")
            val firstName = if (nameArr != null) nameArr[0] else EMPTY
            showToast(getString(R.string.feature_locked, firstName), Toast.LENGTH_LONG)
        }
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
        paymentManager.initializePaymentGateway()
    }

    override fun onCreated() {
        viewModel.testId = testId

        errorView = Stub(findViewById(R.id.error_view))
        addObserver()
        openCourseListener = this
        if (Utils.isInternetAvailable()) {
            viewModel.getBuyPageFeature()
            viewModel.getCoursePriceList(null, null,null)
            viewModel.getValidCouponList(OFFERS, Integer.parseInt(testId))
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }

        } else {
            showErrorView()
        }
        viewModel.saveImpressionForBuyPageLayout(OPEN_BUY_PAGE_LAYOUT, flowFrom)

        NotificationUtils(this).updateNotificationDb(NotificationCategory.AFTER_BUY_PAGE)
        MarketingAnalytics.openPreCheckoutPage()
        initToolbar()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                OPEN_COUPON_LIST_SCREEN -> {
                    openCouponList()
                }
                BUY_COURSE_LAYOUT_DATA -> {
                    try {
                        paymentButtonValue = (it.obj as BuyCourseFeatureModelNew).paymentButtonText
                        paymentButton(it.obj as BuyCourseFeatureModelNew)
                        dynamicCardCreation(it.obj as BuyCourseFeatureModelNew)
                        clickRatingOpen?.setOnClickListener {
                            openRatingAndReviewScreen()
                        }
                        setFreeTrialTimer(it.obj as BuyCourseFeatureModelNew)
                    } catch (ex: Exception) {

                    }
                }
                CLICK_ON_PRICE_CARD -> {
                    setCoursePrices(it.obj as CourseDetailsList, it.arg1)
                }
                CLICK_ON_COUPON_APPLY -> {
                    Log.e("sagar", "initViewState: 1" )
                    val coupon = it.obj as Coupon
                    updateListItem(coupon)
                    couponApplied(coupon, COUPON_APPLY_POP_UP_SHOW_AND_BACK, 0)
                }
                APPLY_COUPON_FROM_BUY_PAGE -> {
                    Log.e("sagar", "initViewState: 3")
                    val coupon = it.obj as Coupon
                    onCouponApply(coupon)
                }
                APPLY_COUPON_FROM_INTENT -> {
                    if (shouldAutoApplyCoupon) {
                        viewModel.couponList?.firstOrNull { coupon -> coupon.isAutoApply == true }?.let { coupon ->
                            viewModel.saveImpression(L2_COUPON_AUTO_APPLIED)
                            viewModel.applyEnteredCoupon(coupon.couponCode, NORMAL_BACK_PRESS, 0)
                        }
                    }
                    if (couponCodeFromIntent.isNullOrEmpty().not())
                        viewModel.applyEnteredCoupon(couponCodeFromIntent!!, NORMAL_BACK_PRESS, 0)
                }
                OPEN_COURSE_EXPLORE -> openCourseExplorerActivity()
                MAKE_PHONE_CALL -> openSalesReasonScreenOrMakeCall()
                BUY_PAGE_BACK_PRESS -> popBackStack()
                APPLY_COUPON_BUTTON_SHOW -> showApplyButton()
                COUPON_APPLIED -> couponApplied(it.obj as Coupon, it.arg1, it.arg2)
                SCROLL_TO_BOTTOM -> {
                    if (!viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V1_ENABLED) && !viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V2_ENABLED)){
                        binding.btnCallUs.post {
                            binding.scrollView.smoothScrollTo(
                                binding.buyPageParentContainer.width,
                                binding.buyPageParentContainer.height,
                                2000
                            )
                        }
                    }
                }
                PAYMENT_SUCCESS -> onPaymentSuccess()
                PAYMENT_FAILED -> showPaymentFailedDialog()
                PAYMENT_PENDING -> showPendingDialog()
                GET_USER_COUPONS_API_ERROR ->{
                val map = it.obj as HashMap<*, *>
                    openErrorScreen(errorCode = GET_USER_COUPONS_API_ERROR.toString(), map)
                }
                COURSE_PRICE_LIST_ERROR -> {
                    val map = it.obj as HashMap<*, *>
                    openErrorScreen(errorCode = COURSE_PRICE_LIST_ERROR.toString(), map)
                }
                CREATE_ORDER_V3_ERROR -> {
                    Log.e("sagar", "CREATE_ORDER_V3_ERROR: ", )
                    val map = it.obj as HashMap<*, *>
                    openErrorScreen(errorCode = CREATE_ORDER_V3_ERROR.toString(), map)
                }
                CLICK_ON_TESTIMONIALS_VIDEO ->{
                    Log.e("sagar", "initViewState: ${CLICK_ON_TESTIMONIALS_VIDEO}", )
                    val obj = it.obj as TestimonialVideo
                    VideoPlayerActivity.startVideoActivity(
                        context = this,
                        videoTitle = null,
                        videoId = obj.id.toString(),
                        videoUrl = obj.video_url
                    )
                }
            }
        }
    }

    fun addObserver() {
        viewModel.apiStatus.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    errorView?.resolved()?.let {
                        errorView!!.get().onSuccess()
                    }
                }
                else -> {}
            }
        }
    }

    private fun couponApplied(coupon: Coupon, isFromLink: Int?, isApplyFrom:Int) {
        showToast("Coupon applied")
        Log.e("sagar", "couponApplied: $isFromLink")
        when (isFromLink) {
            COUPON_APPLY_POP_UP_SHOW_AND_BACK -> {
                onBackPressed()
                if (isApplyFrom == 0)
                    onCouponApply(coupon)
            }
            null -> {
                onBackPressed()
            }
            else -> {
                binding.btnCallUs.post {
                    binding.scrollView.smoothScrollTo(
                        binding.buyPageParentContainer.width,
                        binding.buyPageParentContainer.height
                    )
                }
            }
        }
    }

    private fun setFreeTrialTimer(buyCourseFeatureModel: BuyCourseFeatureModelNew) {
        if ((buyCourseFeatureModel.expiryTime?.time ?: 0) >= System.currentTimeMillis()) {
            startTimer((buyCourseFeatureModel.expiryTime?.time ?: 0) - System.currentTimeMillis())
        } else {
            PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            openCourseListener?.onFreeTrialEnded() // correct
            countdownTimerBack?.stop()
        }
    }

    private fun openSalesReasonScreenOrMakeCall() {
        viewModel.saveImpressionForBuyPageLayout(BUY_PAGE_CALL_CLICKED)
        if (isCallUsButtonActive == 1) {
            Utils.call(
                this@BuyPageActivity,
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(BUY_PAGE_SUPPORT_PHONE_NUMBER)
            )
        } else if (isCallUsButtonActive == 2) {
            try {
                openReasonScreen()
            } catch (ex: Exception) {
                Log.e("sagar", "openSalesReasonScreenOrMakeCall:${ex.message} ")
            }
        }
    }

    fun openReasonScreen() {
        try {
            viewModel.saveImpression("TALK_TO_COUNSELOR")
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.buy_page_parent_container, BookACallFragment(), "BookACallFragment")
                addToBackStack("BookACallFragment")
            }
        } catch (ex: Exception) {
            Log.e("sagar", "openReasonScreen: ${ex.message}")
        }
    }

    private fun updateListItem(coupon: Coupon) {
        viewModel.applyCoupon(coupon)
        Log.e("Sagar", "updateListItem: ${viewModel.couponList}")
    }

    private fun setCoursePrices(list: CourseDetailsList, position: Int) {
        priceForPaymentProceed = list
        proceedButtonCard?.findViewById<MaterialButton>(R.id.btn_payment_course)?.text =
            if (paymentButtonValue == 0)
                "Pay ${priceForPaymentProceed?.discountedPrice.toString() ?: "Pay ₹499"}"
            else "Proceed to Payment"
    }

    private fun openCouponList() {
        viewModel.saveImpressionForBuyPageLayout(OPEN_COUPON_PAGE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.buy_page_parent_container, CouponCardFragment(), "CouponCardFragment")
            addToBackStack("CouponCardFragment")
        }
    }

    private fun openRatingAndReviewScreen() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.buy_page_parent_container,
                RatingAndReviewFragment.newInstance(testId.toInt()),
                COURSE_CONTENT
            )
            addToBackStack(COURSE_CONTENT)
        }
    }

    private fun dynamicCardCreation(buyCourseFeatureModel: BuyCourseFeatureModelNew) {
        isCallUsButtonActive = buyCourseFeatureModel.isCallUsActive ?: 0
        binding.shimmer1.visibility = View.GONE
        binding.shimmer1.stopShimmer()
        binding.shimmer1Layout.visibility = View.VISIBLE
        binding.shimmer2.visibility = View.GONE
        binding.shimmer2.stopShimmer()
        val courseDetailsInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        englishCourseCard =
            courseDetailsInflate.inflate(R.layout.english_course_card, null, true)
        binding.courseTypeContainer.removeAllViews()

        val image = englishCourseCard?.findViewById<ImageView>(R.id.img_skill_logo)
        image?.setImage(buyCourseFeatureModel.otherCourseImage ?: EMPTY)

        val text = englishCourseCard?.findViewById<TextView>(R.id.sample_text)
        text?.text = buyCourseFeatureModel.courseName
        binding.courseTypeContainer.addView(englishCourseCard)


        setRating(buyCourseFeatureModel)

        buyCourseFeatureModel.information?.forEach { it ->
            val view = getCourseDescriptionList(it)
            if (view != null) {
                binding.courseDescList.addView(view)
            }
        }
    }

    private fun setSlider(buyCourseFeatureModel:BuyCourseFeatureModelNew){
        adapter.addListOfImages(buyCourseFeatureModel.images)
        binding.sliderViewPager.adapter = adapter

        dotsCount = (binding.sliderViewPager.adapter as BuyPageViewPager).count
        dots = arrayOfNulls(dotsCount)

        for (i in 0 until dotsCount) {
            dots[i] = ImageView(this)
            dots[i]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, com.joshtalks.joshskills.R.drawable.non_active_dot))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(8, 0, 8, 0)
            binding.indicator.addView(dots[i], params)
        }

        dots[0]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, com.joshtalks.joshskills.R.drawable.active_dot))

        binding.sliderViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                for (i in 0 until dotsCount) {
                    dots[i]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, com.joshtalks.joshskills.R.drawable.non_active_dot))
                }
                dots[position]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, com.joshtalks.joshskills.R.drawable.active_dot))
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    var i = binding.sliderViewPager.currentItem
                    if (i == buyCourseFeatureModel.images.size - 1){
                        i=0
                        binding.sliderViewPager.setCurrentItem(i,true)
                    }else{
                        i++
                        binding.sliderViewPager.setCurrentItem(i,true)
                    }
                }
            }
        }, 4000,4000)
    }

    private fun getCourseDescriptionList(buyCourseFeatureModel: String): View? {
        val courseDescListInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        courseDescListCard =
            courseDescListInflate.inflate(R.layout.item_course_description, null, true)

        val courseDesc = courseDescListCard?.findViewById<TextView>(R.id.course_desc)
        courseDesc?.text = buyCourseFeatureModel
        return courseDescListCard
    }

    private fun setRating(buyCourseFeatureModel: BuyCourseFeatureModelNew) {
        val ratingInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        teacherRatingAndReviewCard =
            ratingInflate.inflate(R.layout.teacher_rating_and_review_card, null, false)

        if (viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V1_ENABLED)){
            showShimmerTestimonials1(true)
            showShimmerTestimonials2(false)

            binding.teacherRatingAndReview.removeAllViews()
            binding.teacherRatingAndReview.addView(teacherRatingAndReviewCard)

            binding.testimonials.visibility = View.VISIBLE
            binding.view13.visibility = View.VISIBLE
            binding.sliderViewPager.visibility = View.VISIBLE
            binding.indicator.visibility = View.VISIBLE
            binding.courseTypeContainer.visibility = View.GONE

            binding.shimmer2Layout.visibility = View.GONE
            setSlider(buyCourseFeatureModel)

        }else if (viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V2_ENABLED)){

            showShimmerTestimonials1(false)
            showShimmerTestimonials2(true)

            binding.teacherRatingAndReview1.removeAllViews()
            binding.teacherRatingAndReview1.addView(teacherRatingAndReviewCard)

            binding.sliderViewPager.visibility = View.VISIBLE
            binding.indicator.visibility = View.VISIBLE
            binding.courseTypeContainer.visibility = View.GONE

            binding.shimmer2Layout.visibility = View.GONE
            setSlider(buyCourseFeatureModel)

        }else{
            showShimmerTestimonials1(true)
            showShimmerTestimonials2(false)

            binding.teacherRatingAndReview.removeAllViews()
            binding.teacherRatingAndReview.addView(teacherRatingAndReviewCard)

            binding.sliderViewPager.visibility = View.GONE
            binding.indicator.visibility = View.GONE
            binding.courseTypeContainer.visibility = View.VISIBLE

            binding.shimmer2Layout.visibility = View.VISIBLE

        }

        val ratingCount = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_count)
        ratingCount?.text = buyCourseFeatureModel.ratingCount.toString() + " Reviews"

        clickRatingOpen = teacherRatingAndReviewCard?.findViewById(R.id.btn_ration_click)

        val teacherRating =
            teacherRatingAndReviewCard?.findViewById<JoshRatingBar>(R.id.teacher_rating)
        teacherRating?.rating = buyCourseFeatureModel.rating ?: 0.0f

        val ratingInText = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_in_text)
        ratingInText?.text = buyCourseFeatureModel.rating.toString() + " out of 5"
    }

    private fun showShimmerTestimonials1(isLayoutVisible:Boolean){
        if (isLayoutVisible)
            binding.shimmerLayoutForTestimonials1.visibility = View.VISIBLE

        binding.shimmerForTestimonials1.visibility = View.GONE
        binding.shimmerForTestimonials1.stopShimmer()
    }

    private fun showShimmerTestimonials2(isLayoutVisible:Boolean){
        if (isLayoutVisible)
            binding.shimmerLayoutForTestimonials2.visibility = View.VISIBLE

        binding.shimmerForTestimonials2.visibility = View.GONE
        binding.shimmerForTestimonials2.stopShimmer()
    }

    private fun paymentButton(buyCourseFeatureModel: BuyCourseFeatureModelNew) {
        val paymentInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        proceedButtonCard = paymentInflate.inflate(R.layout.payment_button_card, null, true)
        val paymentButton = proceedButtonCard?.findViewById<MaterialButton>(R.id.btn_payment_course)
        val paymentButtonLiveMessage = proceedButtonCard?.findViewById<TextView>(R.id.live_message_text)
        if (viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V1_ENABLED) || viewModel.abTestRepository.isVariantActive(VariantKeys.NEW_BUY_PAGE_V2_ENABLED)){
            paymentButtonLiveMessage?.visibility = View.VISIBLE
            paymentButtonLiveMessage?.text = getRandomString(buyCourseFeatureModel.liveMessages)
        }
        binding.paymentProceedBtnCard.removeAllViews()
        binding.paymentProceedBtnCard.addView(proceedButtonCard)
        paymentButton?.setOnSingleClickListener {
            isPaymentInitiated = true
            viewModel.saveImpressionForBuyPageLayout(PROCEED_PAYMENT_CLICK, testId)
            startPayment()
        }
        proceedButtonCard?.findViewById<MaterialTextView>(R.id.text_view_privacy)
            ?.setOnSingleClickListener {
                showPrivacyPolicyDialog()
            }
    }

    private fun getRandomString(list: List<String>?):String{
        return list?.get(Random().nextInt(list.size)) ?: "Mukesh"
    }

    private fun startPayment() {
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        var phoneNumber = getPhoneNumber()
        if (phoneNumber.isEmpty()) {
            phoneNumber = "+919999999999"
        }
        NotificationUtils(applicationContext).removeScheduledNotification(NotificationCategory.AFTER_BUY_PAGE)
        NotificationUtils(applicationContext).updateNotificationDb(NotificationCategory.PAYMENT_INITIATED)
        try {
            paymentManager.createOrder(
                phoneNumber,
                priceForPaymentProceed?.encryptedText ?: EMPTY,
                priceForPaymentProceed?.testId ?: EMPTY
            )
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
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
                R.id.buy_page_parent_container,
                PaymentFailedDialogNew.newInstance(paymentManager),
                "Payment Failed"
            )
            disallowAddToBackStack()
        }
        isPaymentInitiated = false
    }

    private fun navigateToStartCourseActivity() {
        try {
            StartCourseActivity.openStartCourseActivity(
                this,
                priceForPaymentProceed?.courseName ?: EMPTY,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.TEACHER_NAME.plus(
                        PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
                ),
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.TEACHER_IMAGE_URL.plus(
                        PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
                ),
                paymentManager.getJoshTalksId(),
                priceForPaymentProceed?.discountedPrice.toString()
            )
            this.finish()
        } catch (ex: Exception) {
            com.joshtalks.joshskills.premium.core.showToast(getString(R.string.something_went_wrong))
            ex.printStackTrace()
        }
    }

    companion object {
        fun startBuyPageActivity(
            activity: Activity,
            testId: String,
            flowFrom: String,
            coupon: String = EMPTY,
            shouldAutoApplyCoupon: Boolean = false
        ) {
            Intent(activity, BuyPageActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(FLOW_FROM, flowFrom)
                putExtra(COUPON_CODE, coupon)
                putExtra(SHOULD_AUTO_APPLY_COUPON_ARG, shouldAutoApplyCoupon)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(
                    R.anim.slide_up_dialog,
                    R.anim.slide_out_top
                )
            }
        }
    }

    fun getConversationId(): String? {
        return null
    }

    private fun openCourseExplorerActivity() {
        viewModel.saveImpressionForBuyPageLayout(OPEN_COURSE_EXPLORER_SCREEN)
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            null,
            state = com.joshtalks.joshskills.premium.core.BaseActivity.ActivityEnum.BuyPage,
            isClickable = false
        )
    }

    private fun initToolbar() {
        //balanceTv = findViewById<TextView>(R.id.iv_earn)
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                popBackStack()
            }
        }
    }

    private fun showApplyButton() {
        with(findViewById<View>(R.id.btn_apply_coupon)) {
            isVisible = !viewModel.isOfferOrInsertCodeVisible.get()
            setOnClickListener {
                openCouponList()
            }
        }
    }

    private fun popBackStack() {
        try {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressed()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    private fun onCouponApply(coupon: Coupon) {
        Log.e("sagar", "onCouponApply:")
        val dialogView = showCustomDialog(R.layout.coupon_applied_alert_dialog)
        val btnGotIt = dialogView.findViewById<AppCompatTextView>(R.id.got_it_button)
        val couponAppleLottie = dialogView.findViewById<LottieAnimationView>(R.id.card_confetti)
        dialogView.setCancelable(true)
        couponAppleLottie.visibility = View.VISIBLE
        couponAppleLottie.playAnimation()
        dialogView.findViewById<TextView>(R.id.coupon_name_text).text =
            coupon.couponCode + " applied"
        dialogView.findViewById<TextView>(R.id.coupon_price_text).text =
            "upto ₹" + coupon.maxDiscountAmount.toString() + " saved with this coupon "

        btnGotIt.setOnClickListener {
            couponAppleLottie.visibility = View.GONE
            couponAppleLottie.pauseAnimation()
            dialogView.dismiss()
        }
    }

    private fun showCustomDialog(view: Int): Dialog {
        val dialogView = Dialog(this)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.setCancelable(true)
        dialogView.setContentView(view)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.show()
        return dialogView
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack?.stop()
        countdownTimerBack = null
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                openCourseListener?.onStartTrialTimer(millis)
            }

            override fun onTimerFinish() {
                PrefManager.put(IS_FREE_TRIAL, true)
                openCourseListener?.onFreeTrialEnded()
                countdownTimerBack?.stop()
            }
        }
        countdownTimerBack?.startTimer()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        showWebViewDialog(url)
    }

    private fun showWebViewDialog(webUrl: String) {
        WebViewFragment.showDialog(supportFragmentManager, webUrl)
    }

    fun showProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    fun hideProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.GONE
        }
    }

    fun showErrorView() {
        errorView?.resolved().let {
            errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                override fun onRetryButtonClicked() {
                    if (Utils.isInternetAvailable()) {
                        viewModel.getBuyPageFeature()
                        viewModel.getCoursePriceList(null, null,null)
                        viewModel.getValidCouponList(OFFERS, Integer.parseInt(testId))
                    } else {
                        errorView?.get()?.enableRetryBtn()
                        Snackbar.make(
                            binding.root,
                            getString(R.string.internet_not_available_msz),
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(getString(R.string.settings)) {
                                startActivity(
                                    Intent(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                        else
                                            Settings.ACTION_WIRELESS_SETTINGS
                                    )
                                )
                            }.show()
                    }
                }
            })
        }
    }

    override fun onProcessStart() {
        showProgressBar()
    }

    override fun onProcessStop() {
        viewModel.saveImpressionForBuyPageLayout(GATEWAY_INITIALISED, "BUY_PAGE_LAYOUT")
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
            replace(R.id.buy_page_parent_container, fragment, "Payment Processing")
            disallowAddToBackStack()
        }
    }

    override fun onEvent(data: JSONObject?) {
        data?.let {
            viewModel.logPaymentEvent(data)
        }
    }

    private fun showPendingDialog() {
        PrefManager.put(STICKY_COUPON_DATA, EMPTY)
        stopService(Intent(this, StickyNotificationService::class.java))
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentPendingFragment()
            replace(R.id.buy_page_parent_container, fragment, "Payment Pending")
            disallowAddToBackStack()
        }
    }

    override fun onWarmUpEnded(error: String?) {

    }

    private fun onPaymentSuccess() {
        if (viewModel.isDiscount) {
            viewModel.saveImpression(IMPRESSION_PAY_DISCOUNT)
        } else {
            viewModel.saveImpression(IMPRESSION_PAY_FULL_FEES)
        }
        val freeTrialTestId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
            Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
        } else {
            PrefManager.getStringValue(PAID_COURSE_TEST_ID)
        }

        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
            PrefManager.put(IS_FREE_TRIAL, false)
            PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
        }
        viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        viewModel.saveBranchPaymentLog(
            paymentManager.getJustPayOrderId(),
            BigDecimal(priceForPaymentProceed?.discountedPrice?.toString()),
            testId = Integer.parseInt(freeTrialTestId),
            courseName = priceForPaymentProceed?.courseName ?: EMPTY,
        )
        MarketingAnalytics.coursePurchased(
            BigDecimal(priceForPaymentProceed?.discountedPrice?.toString()),
            true,
            testId = freeTrialTestId,
            courseName = priceForPaymentProceed?.courseName ?: EMPTY,
            juspayPaymentId = paymentManager.getJustPayOrderId()
        )
        try {
            PrefManager.put(STICKY_COUPON_DATA, EMPTY)
            NotificationUtils(applicationContext).removeAllScheduledNotification()
            WorkManagerAdmin.removeStickyNotificationWorker()
            stopService(Intent(this, StickyNotificationService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            NotificationUtils(applicationContext).removeAllNotifications()
        }

        AppObjectController.uiHandler.postDelayed({
            PrefManager.put(IS_PAYMENT_DONE, true)
            navigateToStartCourseActivity()
        }, 2 * 1000L)
    }

    override fun onBackPressed() {
        if (!isPaymentInitiated) {
            super.onBackPressed()
        } else {
            backPressMutex.onMultipleBackPress {
                val backPressHandled = paymentManager.getJuspayBackPress()
                if (!backPressHandled) {
                    viewModel.saveImpressionForBuyPageLayout(BACK_PRESSED_ON_GATEWAY, "BUY_PAGE_LAYOUT")
                    super.onBackPressed()
                } else {
                    viewModel.saveImpressionForBuyPageLayout(BACK_PRESSED_ON_LOADING, "BUY_PAGE_LAYOUT")
                }
            }
        }
    }

    override fun onClick(inboxEntity: InboxEntity) {
        //TODO("Not yet implemented")
    }

    override fun onStartTrialTimer(startTimeInMilliSeconds: Long) {
        binding.freeTrialTimerNewUi.visible()
        binding.freeTrialTimerNewUi.startTimer(startTimeInMilliSeconds)
    }

    override fun onStopTrialTimer() {
        binding.freeTrialTimerNewUi.removeTimer()
    }

    override fun onFreeTrialEnded() {
        Log.d("sagar", "onFreeTrialEnded() called")
        binding.freeTrialTimerNewUi.visible()
        binding.freeTrialTimerNewUi.endFreeTrial()
    }

    private fun openErrorScreen(errorCode:String, map:HashMap<*, *>){
        ErrorActivity.showErrorScreen(
            errorTitle = "Something went wrong",
            errorSubtitle = getString(R.string.error_message_screen),
            errorCode = errorCode,
            activity = this@BuyPageActivity,
            payload = map["payload"].toString(),
            exception = map["exception"].toString()
        )
    }
}