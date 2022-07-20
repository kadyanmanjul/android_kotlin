package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics.logNewPaymentPageOpened
import com.joshtalks.joshskills.databinding.ActivityFreeTrialPaymentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.PromoCodeSubmitEventBus
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.EnterReferralCodeFragment
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.voip.CallForceDisconnect
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.singular.sdk.Singular
import com.tonyodev.fetch2core.isNetworkAvailable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.math.BigDecimal

const val FREE_TRIAL_PAYMENT_TEST_ID = "102"
const val SUBSCRIPTION_TEST_ID = "10"
const val IS_FAKE_CALL = "is_fake_call"

class FreeTrialPaymentActivity : CoreJoshActivity(),
    PaymentResultListener {

    private lateinit var binding: ActivityFreeTrialPaymentBinding
    private val viewModel: FreeTrialPaymentViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialPaymentViewModel::class.java)
    }
    private var razorpayOrderId = EMPTY
    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    private var couponApplied = false
    private var compositeDisposable = CompositeDisposable()
    var isDiscount = false
    private var index = 1
    private var isFreemiumActive = false
    private var isPointsScoredMoreThanEqualTo100 = false
    private var isEnglishCardTapped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_payment
        )
        binding.handler = this
        binding.lifecycleOwner = this
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        if (intent.hasExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)) {
            testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
            } else {
                PrefManager.getStringValue(PAID_COURSE_TEST_ID)
            }
        }

        if (intent.hasExtra(IS_FAKE_CALL)) {
            forceDisconnectCall()
            val nameArr = User.getInstance().firstName?.split(" ")
            val firstName = if (nameArr != null) nameArr[0] else EMPTY
            showToast(getString(R.string.feature_locked, firstName), Toast.LENGTH_LONG)
        }
        if (testId.isBlank()) {
            testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
            } else {
                PrefManager.getStringValue(PAID_COURSE_TEST_ID)
            }
        }
        isFreemiumActive =
            viewModel.abTestRepository.isVariantActive(VariantKeys.FREEMIUM_ENABLED) &&
                    PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                if (isFreemiumActive)
                    FreemiumPaymentFragment.newInstance(testId)
                else FreeTrialPaymentFragment.newInstance(testId)
            )
            .commit()
        setObservers()
        logNewPaymentPageOpened()
        setListeners()
        Singular.event(SingularEvent.OPENED_FREE_TRIAL_PAYMENT.name, testId)
    }


    private fun forceDisconnectCall() {
        val serviceIntent = Intent(
            this,
            WebRtcService::class.java
        ).apply {
            action = CallForceDisconnect().action
            putExtra(IS_FAKE_CALL, true)
        }
        serviceIntent.startServiceForWebrtc()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        super.onBackPressed()
    }

    private fun setListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressed()
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        }
        binding.applyCoupon.setOnClickListener {
            try {
                MixPanelTracker.publishEvent(MixPanelEvent.APPLY_COUPON_CLICKED)
                    .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                    .addParam(
                        ParamKeys.COURSE_PRICE,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                    )
                    .addParam(
                        ParamKeys.COURSE_NAME,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                    )
                    .addParam(
                        ParamKeys.COURSE_ID,
                        PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                    )
                    .push()
                viewModel.saveImpression(IMPRESSION_CLICKED_APPLY_COUPON)
                val bottomSheetFragment = EnterReferralCodeFragment.newInstance(true)
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun enableBuyCourseButton() {
        binding.materialTextView.isEnabled = true
        binding.materialTextView.alpha = 1f
    }

    private fun disableBuyCourseButton() {
        binding.materialTextView.isEnabled = false
        binding.materialTextView.alpha = 0.5f
    }


    private fun openCourseExploreActivity() {
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            null,
            state = ActivityEnum.FreeTrial,
            isClickable = false
        )
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(PromoCodeSubmitEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.promoCode.isNullOrEmpty().not()) {
                        showProgressBar()
                        couponApplied = true
                        if (isFreemiumActive)
                            viewModel.getFreemiumPaymentDetails(testId, it.promoCode!!)
                        else
                            viewModel.getPaymentDetails(testId.toInt(), it.promoCode!!)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }


    private fun setObservers() {
        viewModel.orderDetailsLiveData.observe(this) {
            initializeRazorpayPayment(it)
        }
        viewModel.isProcessing.observe(this) { isProcessing ->
            if (isProcessing) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        }
        viewModel.paymentButtonText.observe(this) { it ->
            binding.materialTextView.text = it
        }
        viewModel.event.observe(this) {
            when (it.what) {
                ENABLE_BUY_BUTTON -> enableBuyCourseButton()
                DISABLE_BUY_BUTTON -> disableBuyCourseButton()
                HIDE_PROGRESS_BAR -> hideProgressBar()
                SHOW_PROGRESS_BAR -> showProgressBar()
                OPEN_COURSE_EXPLORE_ACTIVITY -> openCourseExploreActivity()
                ENGLISH_CARD_TAPPED -> isEnglishCardTapped = true
                SUBSCRIPTION_CARD_TAPPED -> isEnglishCardTapped = false
                ERROR_OCCURRED -> hideProgressBar()//todo error occurred
            }
        }
        if (!isNetworkAvailable()) {
            binding.applyCoupon.visibility = View.INVISIBLE
            binding.floatingContainer.visibility = View.GONE
            binding.toolbar.visibility = View.VISIBLE
            binding.noInternet.visibility = View.VISIBLE
            binding.parentContainer.setBackgroundColor(Color.WHITE)
        }
        viewModel.index.observe(this) {
            index = it
        }
        viewModel.freemiumPaymentDetailsLiveData.observe(this) {
            if (couponApplied && it.couponDetails != null) {
                doOnCouponApplied(it.couponDetails.isPromoCode, it.couponDetails.header)
            }
        }
        viewModel.paymentDetailsLiveData.observe(this) {
            try {
                if (it.totalPoints > 100) {
                    isPointsScoredMoreThanEqualTo100 = true
                }
                if (couponApplied) {
                    doOnCouponApplied(it.couponDetails.isPromoCode, it.couponDetails.header)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun doOnCouponApplied(isSuccessful: Boolean, headerText: String) {
        hideProgressBar()
        when (isSuccessful) {
            true -> {
                MixPanelTracker.publishEvent(MixPanelEvent.COUPON_APPLIED)
                    .addParam(
                        ParamKeys.TEST_ID,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId
                    )
                    .addParam(
                        ParamKeys.COURSE_PRICE,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.actualAmount
                    )
                    .addParam(
                        ParamKeys.DISCOUNTED_AMOUNT,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                    )
                    .addParam(
                        ParamKeys.COURSE_NAME,
                        viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                    )
                    .addParam(
                        ParamKeys.COURSE_ID,
                        PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                    )
                    .push()

                showToast("Coupon Applied Successfully")
                viewModel.saveImpression(IMPRESSION_APPLY_COUPON_SUCCESS)
                binding.discount.text = headerText
                binding.applyCoupon.text = getString(R.string.coupon_applied)
                binding.discount.visibility = View.VISIBLE
                binding.applyCoupon.isClickable = false
                isDiscount = true
            }
            false -> {
                MixPanelTracker.publishEvent(MixPanelEvent.APPLY_COUPON_FAILED)
                    .addParam(ParamKeys.TEST_ID, testId)
                    .push()
                showToast(getString(R.string.invalid_coupon_code))
            }
        }
    }

    private fun initializeRazorpayPayment(orderDetails: OrderDetailResponse) {
        binding.progressBar.visibility = View.VISIBLE
        val checkout = Checkout()
        checkout.setImage(R.mipmap.ic_launcher)
        checkout.setKeyID(orderDetails.razorpayKeyId)
        try {
            val preFill = JSONObject()

            if (User.getInstance().email.isNullOrEmpty().not()) {
                preFill.put("email", User.getInstance().email)
            } else {
                preFill.put("email", Utils.getUserPrimaryEmail(applicationContext))
            }
            //preFill.put("contact", "9999999999")

            val options = JSONObject()
            options.put("key", orderDetails.razorpayKeyId)
            options.put("name", "Josh Skills")
            try {
                options.put(
                    "description",
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName + "_app"
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            options.put("order_id", orderDetails.razorpayOrderId)
            options.put("currency", orderDetails.currency)
            options.put("amount", orderDetails.amount * 100)
            options.put("prefill", preFill)
            checkout.open(this, options)
            razorpayOrderId = orderDetails.razorpayOrderId
            binding.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPayment() {
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        var phoneNumber = getPhoneNumber()
        if (phoneNumber.isEmpty()) {
            phoneNumber = "+919999999999"
        }

        if (PrefManager.getStringValue(CURRENT_COURSE_ID) != DEFAULT_COURSE_ID)
            index = 0
        viewModel.getOrderDetails(
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId
                ?: viewModel.freemiumPaymentDetailsLiveData.value?.testId ?: testId,
            phoneNumber,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.encryptedText
                ?: viewModel.freemiumPaymentDetailsLiveData.value?.encryptedText ?: EMPTY
        )

        MixPanelTracker.publishEvent(MixPanelEvent.PAYMENT_STARTED)
            .addParam(ParamKeys.AMOUNT_PAID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount)
            .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
            .addParam(ParamKeys.COURSE_NAME, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName)
            .addParam(ParamKeys.IS_COUPON_APPLIED, viewModel.paymentDetailsLiveData.value?.couponDetails?.isPromoCode)
            .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
            .push()

        val jsonData = JSONObject()
        jsonData.put(ParamKeys.TEST_ID.name, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
        jsonData.put(
            ParamKeys.COURSE_PRICE.name,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
        )
        Singular.eventJSON(SingularEvent.INITIATED_PAYMENT.name, jsonData)
    }

    private fun String.verifyPayment() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf("razorpay_order_id" to this@verifyPayment)
                AppObjectController.commonNetworkService.verifyPayment(data)
            } catch (ex: HttpException) {
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        // isBackPressDisabled = true
        viewModel.mentorPaymentStatus.observe(this) {
            when (it) {
                true -> {
                    if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                        PrefManager.put(IS_DEMO_P2P, false)
                    }
                    val freeTrialTestId =
                        Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
                    if (testId == freeTrialTestId) {
                        PrefManager.put(IS_COURSE_BOUGHT, true)
                        PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
                    }
                    // isBackPressDisabled = true
                    razorpayOrderId.verifyPayment()
                    MarketingAnalytics.coursePurchased(BigDecimal(viewModel.orderDetailsLiveData.value?.amount ?: 0.0))
                    //viewModel.updateSubscriptionStatus()

                    uiHandler.post {
                        PrefManager.put(IS_PAYMENT_DONE, true)
                        showPaymentProcessingFragment()
                    }

                    uiHandler.postDelayed({
                        navigateToStartCourseActivity()
                    }, 1000L * 5L)
                }
                false -> {
                    uiHandler.post {
                        showPaymentFailedDialog()
                    }
                }
            }
        }
        try {
            MixPanelTracker.publishEvent(MixPanelEvent.PAYMENT_FAILED)
                .addParam(
                    ParamKeys.AMOUNT_PAID,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                )
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                .addParam(
                    ParamKeys.COURSE_NAME,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                )
                .addParam(
                    ParamKeys.IS_COUPON_APPLIED,
                    viewModel.paymentDetailsLiveData.value?.couponDetails?.isPromoCode
                )
                .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                .push()

            val jsonData = JSONObject()
            jsonData.put(ParamKeys.TEST_ID.name, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
            jsonData.put(
                ParamKeys.AMOUNT_PAID.name,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
            )
            jsonData.put(
                ParamKeys.IS_COUPON_APPLIED.name,
                viewModel.paymentDetailsLiveData.value?.couponDetails?.isPromoCode
            )
            Singular.eventJSON(SingularEvent.PAYMENT_FAILED.name, jsonData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        if (viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId == FREE_TRIAL_PAYMENT_TEST_ID) {
            if (viewModel.abTestRepository.isVariantActive(VariantKeys.ICP_ENABLED))
                viewModel.postGoal("ICP_COURSE_BOUGHT", CampaignKeys.INCREASE_COURSE_PRICE.name)
        } else if (viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId == SUBSCRIPTION_TEST_ID) {
            if (viewModel.abTestRepository.isVariantActive(VariantKeys.ICP_ENABLED))
                viewModel.postGoal("ICP_SUBSCRIPTION_BOUGHT", CampaignKeys.INCREASE_COURSE_PRICE.name)
        }

        val obj = JSONObject()
        obj.put("is paid", true)
        obj.put(
            "is 100 points obtained in free trial",
            viewModel.abTestRepository.isVariantActive(VariantKeys.POINTS_HUNDRED_ENABLED)
        )
        MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
        MixPanelTracker.mixPanel.people.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
        MixPanelTracker.mixPanel.people.set(obj)
        MixPanelTracker.mixPanel.registerSuperProperties(obj)

        if (isDiscount) {
            viewModel.saveImpression(IMPRESSION_PAY_DISCOUNT)
        } else {
            viewModel.saveImpression(IMPRESSION_PAY_FULL_FEES)
        }
        if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
            PrefManager.put(IS_DEMO_P2P, false)
        }
        val freeTrialTestId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
            Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
        } else {
            PrefManager.getStringValue(PAID_COURSE_TEST_ID)
        }

        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
            PrefManager.removeKey(IS_FREE_TRIAL_ENDED)

            if (isEnglishCardTapped && PrefManager.getBoolValue(IS_ENGLISH_SYLLABUS_PDF_OPENED)) {
                viewModel.saveImpression(SYLLABUS_OPENED_AND_ENGLISH_COURSE_BOUGHT)
                viewModel.postGoal(GoalKeys.ESD_COURSE_BOUGHT.name, CampaignKeys.ENGLISH_SYLLABUS_DOWNLOAD.name)

            }

            if (isEnglishCardTapped && isPointsScoredMoreThanEqualTo100 && viewModel.abTestRepository.isVariantActive(
                    VariantKeys.POINTS_HUNDRED_ENABLED
                )
            ) {
                viewModel.saveImpression(POINTS_100_OBTAINED_ENGLISH_COURSE_BOUGHT)
                viewModel.postGoal(GoalKeys.HUNDRED_POINTS_COURSE_BOUGHT.NAME, CampaignKeys.HUNDRED_POINTS.NAME)
            }
        }
        // isBackPressDisabled = true
        razorpayOrderId.verifyPayment()
        MarketingAnalytics.coursePurchased(
            BigDecimal(
                viewModel.orderDetailsLiveData.value?.amount ?: 0.0
            )
        )
        //viewModel.updateSubscriptionStatus()
        try {
            MixPanelTracker.publishEvent(MixPanelEvent.PAYMENT_SUCCESS)
                .addParam(
                    ParamKeys.AMOUNT_PAID,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                )
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                .addParam(
                    ParamKeys.COURSE_NAME,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                )
                .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                .addParam(
                    ParamKeys.IS_COUPON_APPLIED,
                    viewModel.paymentDetailsLiveData.value?.couponDetails?.isPromoCode
                )
                .addParam(ParamKeys.IS_100_POINTS_OBTAINED_IN_FREE_TRIAL, isPointsScoredMoreThanEqualTo100)
                .push()

            val json = JSONObject()
            json.put(ParamKeys.TEST_ID.name, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
            json.put(
                ParamKeys.AMOUNT_PAID.name,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount ?: 0.0
            )
            json.put(
                ParamKeys.IS_COUPON_APPLIED.name,
                viewModel.paymentDetailsLiveData.value?.couponDetails?.isPromoCode
            )
            Singular.customRevenue(SingularEvent.PAYMENT_SUCCESSFUL.name, json)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        uiHandler.post {
            PrefManager.put(IS_PAYMENT_DONE, true)
            showPaymentProcessingFragment()
        }

        uiHandler.postDelayed({
            viewModel.postGoal(GoalKeys.PAYMENT_COMPLETE.NAME, CampaignKeys.FREEMIUM_COURSE.NAME)
            navigateToStartCourseActivity()
        }, 1000L * 5L)
    }

    private fun showPaymentProcessingFragment() {
        // binding.container.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentProcessingFragment.newInstance(),
                "Payment Processing"
            )
            .commitAllowingStateLoss()
    }

    private fun showPaymentFailedDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentFailedDialogFragment.newInstance(
                    viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
                ),
                "Payment Success"
            )
            .commitAllowingStateLoss()
    }

    private fun navigateToStartCourseActivity() {
        if (PrefManager.getStringValue(CURRENT_COURSE_ID) != DEFAULT_COURSE_ID)
            index = 0
        if (isFreemiumActive) {
            StartCourseActivity.openStartCourseActivity(
                this,
                viewModel.freemiumPaymentDetailsLiveData.value?.courseName ?: EMPTY,
                viewModel.freemiumPaymentDetailsLiveData.value?.teacherName ?: EMPTY,
                viewModel.freemiumPaymentDetailsLiveData.value?.imageUrl ?: EMPTY,
                viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0,
                viewModel.freemiumPaymentDetailsLiveData.value?.testId ?: EMPTY,
                viewModel.freemiumPaymentDetailsLiveData.value?.discountedAmount ?: EMPTY
            )
        } else {
            StartCourseActivity.openStartCourseActivity(
                this,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName ?: EMPTY,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.teacherName ?: EMPTY,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.imageUrl ?: EMPTY,
                viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId ?: EMPTY,
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount ?: EMPTY
            )
        }
        this.finish()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    companion object {
        const val EXPIRED_TIME = "expired_time"

        fun startFreeTrialPaymentActivity(
            activity: Activity,
            testId: String,
            expiredTime: Long? = null
        ) {
            Intent(activity, FreeTrialPaymentActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(EXPIRED_TIME, expiredTime)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(
                    R.anim.slide_up_dialog,
                    R.anim.slide_out_top
                )
            }
        }

        fun getFreeTrialPaymentActivityIntent(
            context: Context,
            testId: String,
            expiredTime: Long? = null,
            isFakeCall: Boolean = false
        ) =
            Intent(context, FreeTrialPaymentActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(EXPIRED_TIME, expiredTime)
                if (isFakeCall) {
                    putExtra(IS_FAKE_CALL, isFakeCall)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

    }
}
