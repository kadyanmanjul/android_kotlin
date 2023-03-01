package com.joshtalks.joshskills.ui.payment.order_summary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.IconMarginSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.color
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.constants.PAYMENT_PENDING
import com.joshtalks.joshskills.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.CTA_PAYMENT_SUMMARY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_PAYMENT_BTN_TXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.PAYMENT_SUMMARY_CTA_LABEL_FREE
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.databinding.ActivityPaymentSummaryBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.PromoCodeSubmitEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.PaymentSummaryResponse
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.*
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.ui.referral.EnterReferralCodeFragment
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.special_practice.utils.GATEWAY_INITIALISED
import com.joshtalks.joshskills.ui.special_practice.utils.PROCEED_PAYMENT_CLICK
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt

const val TRANSACTION_ID = "TRANSACTION_ID"
const val ENGLISH_COURSE_TEST_ID = "102"
const val ENGLISH_FREE_TRIAL_1D_TEST_ID = "784"
const val SUBSCRIPTION_TEST_ID = "10"
const val ERROR_MSG = "ERROR_MSG"

class PaymentSummaryActivity : CoreJoshActivity(), PaymentGatewayListener {
    private var prefix: String = EMPTY
    private lateinit var binding: ActivityPaymentSummaryBinding
    private var testId: String = EMPTY
    private val uiHandler = Handler(Looper.getMainLooper())
    lateinit var applyCouponText: AppCompatTextView
    lateinit var multiLineLL: LinearLayout
    private var isEcommereceEventFire = true
    private lateinit var appAnalytics: AppAnalytics
    private var isRequestHintAppearred = false
    private var couponApplied = false
    private var isFromNewFreeTrial = false
    private var compositeDisposable = CompositeDisposable()
    private var loginStartFreeTrial = false
    private var is100PointsObtained = false
    private var isHundredPointsActive = false
    private var event = EventLiveData
    private val backPressMutex = Mutex(false)

    private val viewModel: PaymentSummaryViewModel by lazy {
        ViewModelProvider(this).get(
            PaymentSummaryViewModel::class.java
        )
    }
    private val paymentManager: PaymentManager by lazy {
        PaymentManager(
            this,
            viewModel.viewModelScope,
            this
        )
    }

    companion object {
        fun startPaymentSummaryActivity(
            activity: Activity,
            testId: String,
            hasFreeTrial: Boolean? = null,
            isFromNewFreeTrial: Boolean = false,
            is100PointsObtained: Boolean? = false,
            isHundredPointsActive: Boolean = true
        ) {
            Intent(activity, PaymentSummaryActivity::class.java).apply {
                putExtra(TEST_ID_PAYMENT, testId)
                putExtra(IS_FROM_NEW_FREE_TRIAL, isFromNewFreeTrial)
                hasFreeTrial?.run {
                    putExtra(HAS_FREE_7_DAY_TRIAL, hasFreeTrial)
                }
                is100PointsObtained?.run {
                    putExtra(IS_100_POINTS_OBTAINED, is100PointsObtained)
                }
                putExtra(IS_HUNDRED_POINTS_ACTIVE, isHundredPointsActive)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_top)
            }
        }

        const val TEST_ID_PAYMENT = "test_ID"
        const val HAS_FREE_7_DAY_TRIAL = "7 day free trial"
        const val IS_FROM_NEW_FREE_TRIAL = "IS_FROM_NEW_FREE_TRIAL"
        const val IS_100_POINTS_OBTAINED = "IS_100_POINTS_OBTAINED"
        const val IS_HUNDRED_POINTS_ACTIVE = "IS_HUNDRED_POINTS_ACTIVE"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PAYMENT_SUMMARY_OPENED.NAME)
            .addUserDetails()
            .addBasicParam()
        if (intent.hasExtra(TEST_ID_PAYMENT)) {
            val temp = intent.getStringExtra(TEST_ID_PAYMENT)
            if (temp.isNullOrEmpty()) {
                this.finish()
                return
            }
            this.testId = temp
            appAnalytics.addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, temp)
        }
        isFromNewFreeTrial = intent.getBooleanExtra(IS_FROM_NEW_FREE_TRIAL, false)
        is100PointsObtained = intent.getBooleanExtra(IS_100_POINTS_OBTAINED, false)
        isHundredPointsActive = intent.getBooleanExtra(IS_HUNDRED_POINTS_ACTIVE, false)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment_summary)
        binding.lifecycleOwner = this
        binding.handler = this
        initToolbarView()
        initViewModel()
        subscribeObservers()
        initCountryCode()
        logPaymentAnalyticsEvents()
        paymentManager.initializePaymentGateway()
    }

    private fun initViewModel() {
        viewModel.mTestId = testId
        getPaymentDetails(false, testId)
    }

    private fun logPaymentAnalyticsEvents() {
        AppAnalytics.create(AnalyticsEvent.PAYMENT_SUMMARY_INITIATED.NAME)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, "Course Over View")
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, viewModel.getCourseName())
            .addParam(AnalyticsEvent.SHOWN_COURSE_PRICE.NAME, viewModel.getCourseDiscountedAmount())
            .addParam("test_id", viewModel.getPaymentTestId())
            .addParam(AnalyticsEvent.COURSE_PRICE.NAME, viewModel.getCourseActualAmount()).push()
    }

    private fun logPaymentStatusAnalyticsEvents(status: String, reason: String? = "Completed") {
        AppAnalytics.create(AnalyticsEvent.PAYMENT_STATUS_NEW.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.PAYMENT_STATUS.NAME, status)
            .addParam(AnalyticsEvent.REASON.NAME, reason)
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, viewModel.getCourseName())
            .addParam(AnalyticsEvent.SHOWN_COURSE_PRICE.NAME, viewModel.getCourseDiscountedAmount())
            .addParam("test_id", viewModel.getPaymentTestId())
            .addParam(AnalyticsEvent.COURSE_PRICE.NAME, viewModel.getCourseActualAmount()).push()
    }

    private fun initToolbarView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.order_summary)
        applyCouponText = findViewById<AppCompatTextView>(R.id.apply_coupon)
        applyCouponText.visibility = View.GONE

        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            appAnalytics.addParam(AnalyticsEvent.BACK_PRESSED.NAME, true)
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            this.finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        event.observe(this) {
            when(it.what) {
                PAYMENT_SUCCESS -> onPaymentSuccess()
                PAYMENT_FAILED -> showPaymentFailedDialog()
                PAYMENT_PENDING -> showPendingDialog()
            }
        }
        viewModel.viewState?.observe(this, androidx.lifecycle.Observer {
            when (it) {
                PaymentSummaryViewModel.ViewState.INTERNET_NOT_AVAILABLE -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(getString(R.string.internet_not_available_msz))
                }
                PaymentSummaryViewModel.ViewState.ERROR_OCCURED -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
                }
                PaymentSummaryViewModel.ViewState.PROCESSING -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.container.visibility = View.VISIBLE
                }
            }
        })

        viewModel.responsePaymentSummary.observe(this) { paymentSummaryResponse ->

            val stringListName = paymentSummaryResponse.courseName.split(" with ")
            binding.courseName.text = stringListName.get(0)
            binding.tutorName.text = "with ".plus(paymentSummaryResponse.teacherName)

            val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
            val enrollUser = df.format(paymentSummaryResponse.totalEnrolled)
            binding.enrolled.text = enrollUser.plus("+")
            binding.enrolled.setTypeface(binding.enrolled.typeface, Typeface.BOLD)

            binding.rating.text = String.format("%.1f", paymentSummaryResponse.rating)

            appAnalytics.addParam(
                AnalyticsEvent.COURSE_NAME.NAME,
                paymentSummaryResponse.courseName
            )
            appAnalytics.addParam(
                AnalyticsEvent.COURSE_PRICE.NAME,
                paymentSummaryResponse.discountedAmount
            )

            Glide.with(applicationContext)
                .load(paymentSummaryResponse.imageUrl)
                .fitCenter()
                .into(binding.profileImage)
            binding.txtPrice.text =
                "₹ ${String.format("%.2f", paymentSummaryResponse.amount)}"

            multiLineLL = binding.multiLineLl
            multiLineLL.removeAllViews()

            paymentSummaryResponse.features?.let {
                val stringList = it.split(",")
                if (stringList.isNullOrEmpty().not())
                    stringList.forEach { str ->
                        if (str.isEmpty().not()) multiLineLL.addView(getTextView(str))
                    }
            }
            if (viewModel.getCourseDiscountedAmount() > 0) {
                binding.materialButton.text =
                    "${
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(CTA_PAYMENT_SUMMARY)
                    } ₹ ${
                        viewModel.getCourseDiscountedAmount()
                            .roundToInt()
                    }"
            } else {
                binding.materialButton.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(PAYMENT_SUMMARY_CTA_LABEL_FREE)
            }

            if (isFromNewFreeTrial) {
                binding.materialButton.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FREE_TRIAL_PAYMENT_BTN_TXT)
            }

            if (couponApplied) {
                hideProgressBar()
                when (paymentSummaryResponse.couponDetails.isPromoCode) {
                    true -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.COUPON_APPLIED)
                            .addParam(ParamKeys.TEST_ID, viewModel.getPaymentTestId())
                            .addParam(ParamKeys.COURSE_NAME, viewModel.getCourseName())
                            .addParam(ParamKeys.COURSE_PRICE, viewModel.getCourseActualAmount())
                            .addParam(
                                ParamKeys.DISCOUNTED_AMOUNT,
                                viewModel.getCourseDiscountedAmount()
                            )
                            .addParam(
                                ParamKeys.COURSE_ID,
                                PrefManager.getStringValue(
                                    CURRENT_COURSE_ID,
                                    false,
                                    DEFAULT_COURSE_ID
                                )
                            )
                            .push()

                        showToast("Coupon Applied Successfully")

                        val blackColor = ContextCompat.getColor(this, R.color.text_default)
                        val greenColor = ContextCompat.getColor(this, R.color.success)
                        val text = SpannableStringBuilder()
                            .color(blackColor) { append("Coupon Applied") }
                            .append("\n")
                            .color(greenColor) { append(paymentSummaryResponse.couponDetails.header) }

                        applyCouponText.text =
                            text
                        binding.txtPrice.text =
                            "₹ ${String.format("%.2f", viewModel.getCourseDiscountedAmount())}"
                        binding.actualTxtPrice.visibility = View.VISIBLE
                        binding.actualTxtPrice.text =
                            "₹ ${String.format("%.2f", viewModel.getCourseActualAmount())}"
                        binding.actualTxtPrice.paintFlags =
                            binding.actualTxtPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        applyCouponText.isClickable = false
                    }
                    false -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.APPLY_COUPON_FAILED)
                            .addParam(ParamKeys.TEST_ID, viewModel.getPaymentTestId())
                            .push()
                        showToast(getString(R.string.invalid_coupon_code))
                    }
                }
            } else if (paymentSummaryResponse.couponDetails.title.isEmpty().not()) {
                binding.textView1.text = paymentSummaryResponse.couponDetails.name
                binding.tvTip.text = paymentSummaryResponse.couponDetails.title
                binding.tvTipValid.text = paymentSummaryResponse.couponDetails.validity
                binding.tvTipOff.text = paymentSummaryResponse.couponDetails.header

                appAnalytics.addParam(
                    AnalyticsEvent.SPECIAL_DISCOUNT.NAME,
                    paymentSummaryResponse.couponDetails.title
                )

                binding.badeBhaiyaTipContainer.visibility = View.VISIBLE

                binding.badeBhaiyaTipContainer.setOnClickListener {

                    appAnalytics.addParam(AnalyticsEvent.HAVE_COUPON_CODE.NAME, true)
                    binding.badeBhaiyaTipContainer.visibility = View.GONE
                    binding.txtPrice.text =
                        "₹ ${String.format("%.2f", viewModel.getCourseDiscountedAmount())}"
                    binding.actualTxtPrice.visibility = View.VISIBLE
                    binding.actualTxtPrice.text =
                        "₹ ${String.format("%.2f", viewModel.getCourseActualAmount())}"
                    binding.actualTxtPrice.paintFlags =
                        binding.actualTxtPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    if (viewModel.getCourseDiscountedAmount() > 0) {
                        binding.tipUsedMsg.text = SpannableStringBuilder(
                            getString(
                                R.string.tip_used_info,
                                viewModel.getDiscount().toString()
                            )
                        )
                    } else {
                        binding.tipUsedMsg.text = getString(R.string.coupon_applied_free_course)
                        binding.materialButton.text =
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString("CTA_PAYMENT_SUMMARY_FREE")
                    }

                    binding.tipUsedMsg.visibility = View.VISIBLE
                }
            } else if (paymentSummaryResponse.specialOffer != null) {

                binding.subContainer.visibility = View.VISIBLE
                binding.titleSub.text =
                    HtmlCompat.fromHtml(
                        paymentSummaryResponse.specialOffer.title,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                binding.textSub.text =
                    HtmlCompat.fromHtml(
                        paymentSummaryResponse.specialOffer.description,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                binding.subCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        val obj = JSONObject()
                        obj.put("test id", testId)
                        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_UPGRADED)
                            .addParam(ParamKeys.TEST_ID, testId)
                            .push()

                        showSubscriptionDetails(
                            true,
                            viewModel.responseSubscriptionPaymentSummary.value
                        )
                        binding.addThisTv.visibility = View.GONE
                    } else {
                        showSubscriptionDetails(
                            false,
                            viewModel.responsePaymentSummary.value
                        )
                        binding.addThisTv.visibility = View.VISIBLE
                    }
                }

                getPaymentDetails(true, paymentSummaryResponse.specialOffer.test_id.toString())
            } else if (viewModel.getCourseDiscountedAmount() > 0 &&
                AppObjectController.getFirebaseRemoteConfig()
                    .getBoolean(FirebaseRemoteConfigKey.IS_APPLY_COUPON_ENABLED)
            ) {
                applyCouponText.visibility = View.VISIBLE
                applyCouponText.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.APPLY_COUPON_TEXT)
                applyCouponText.setOnClickListener {
                    MixPanelTracker.publishEvent(MixPanelEvent.APPLY_COUPON_CLICKED)
                        .addParam(ParamKeys.TEST_ID, viewModel.getPaymentTestId())
                        .addParam(ParamKeys.COURSE_NAME, viewModel.getCourseName())
                        .addParam(ParamKeys.COURSE_PRICE, viewModel.getCourseDiscountedAmount())
                        .addParam(
                            ParamKeys.COURSE_ID,
                            PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                        )
                        .push()

                    openPromoCodeBottomSheet()
                }
            }

            binding.materialButton.setOnSingleClickListener {
                startPayment()
            }
        }
        if (viewModel.hasRegisteredMobileNumber) {
            binding.group1.visibility = View.GONE
        }

        viewModel.isFreeOrderCreated.observe(this, androidx.lifecycle.Observer
        {
            if (it) {
                PrefManager.put(IS_PAYMENT_DONE, true)
                if (isFromNewFreeTrial) {
                    if (PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).isBlank())
                        PrefManager.put(
                            PAYMENT_MOBILE_NUMBER,
                            prefix.plus(SINGLE_SPACE).plus(binding.mobileEt.text)
                        )
                    if (User.getInstance().isVerified) {
                        viewModel.saveTrueCallerImpression(IMPRESSION_ALREADY_NEWUSER_STARTED)
                        startActivity(getInboxActivityIntent())
                        this.finish()
                    } else {
                        navigateToLoginActivity()
                    }
                } else {
                    navigateToStartCourseActivity(false)
                }
            }
        })

        binding.mobileEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                startPayment()
            }
            true
        }
    }

    private fun openPromoCodeBottomSheet() {
        val bottomSheetFragment = EnterReferralCodeFragment.newInstance(true)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
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
                        getPaymentDetails(false, testId, it.promoCode)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun showSubscriptionDetails(
        isSubscriptionTipUsed: Boolean,
        data: PaymentSummaryResponse?
    ) {
        if (data == null)
            return

        viewModel.setIsSubscriptionTipUsed(isSubscriptionTipUsed)

        val stringListName = data.courseName.split(" with ")
        binding.courseName.text = stringListName.get(0)
        binding.tutorName.text = "with ".plus(data.teacherName)

        val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
        val enrollUser = df.format(data.totalEnrolled)
        binding.enrolled.text = enrollUser.plus("+")
        binding.enrolled.setTypeface(binding.enrolled.typeface, Typeface.BOLD)

        binding.rating.text = data.rating.toString()

        Glide.with(applicationContext)
            .load(data.imageUrl)
            .fitCenter()
            .into(binding.profileImage)

        binding.txtPrice.text =
            "₹ ${String.format("%.2f", data.amount)}"
        multiLineLL = binding.multiLineLl
        multiLineLL.removeAllViews()

        data.features?.let {
            val stringList = it.split(",")
            if (stringList.isNullOrEmpty().not())
                stringList.forEach {
                    if (it.isEmpty().not()) multiLineLL.addView(getTextView(it))
                }
        }

        if (viewModel.getCourseDiscountedAmount() > 0) {
            binding.materialButton.text =
                "${
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(CTA_PAYMENT_SUMMARY)
                } ₹ ${
                    viewModel.getCourseDiscountedAmount()
                        .roundToInt()
                }"
        } else {
            binding.materialButton.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(PAYMENT_SUMMARY_CTA_LABEL_FREE)
        }

        if (isSubscriptionTipUsed) {

            binding.titleSub.text =
                HtmlCompat.fromHtml(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_BB_TIP),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            binding.textSub.text =
                HtmlCompat.fromHtml(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_BB_TEXT),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

        } else {
            if (data.specialOffer != null) {

                binding.titleSub.text =
                    HtmlCompat.fromHtml(data.specialOffer.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.textSub.text =
                    HtmlCompat.fromHtml(
                        data.specialOffer.description,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )

            }
        }
    }


    private fun getPaymentDetails(isSubscription: Boolean, testId: String, coupon: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                data["test_id"] = testId
                data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID, false)

                if (Mentor.getInstance().getId().isNotEmpty() && User.getInstance().isVerified) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                }
                if (PrefManager.getStringValue(REFERRED_REFERRAL_CODE)
                        .isNotBlank() && isSubscription.not()
                ) {
                    data["coupon"] = PrefManager.getStringValue(REFERRED_REFERRAL_CODE)
                } else if (coupon.isNullOrEmpty().not()) {
                    data["coupon"] = coupon!!
                }
                if (isSubscription) {
                    viewModel.getSubscriptionPaymentDetails(data)
                } else {
                    viewModel.getPaymentSummaryDetails(data)
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    private fun initCountryCode() {
        val supportedCountryList =
            AppObjectController.getFirebaseRemoteConfig().getString("SUPPORTED_COUNTRY_LIST")
        if (supportedCountryList.isNotEmpty()) {
            binding.countryCodePicker.setCustomMasterCountries(supportedCountryList)
        }
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        binding.countryCodePicker.setOnCountryChangeListener {
            prefix =
                binding.countryCodePicker.selectedCountryCodeWithPlus
        }
        val defaultRegion: String = getDefaultCountryIso(this)
        prefix = binding.countryCodePicker.getCountryCodeByName(defaultRegion)
        binding.mobileEt.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                requestHint()
            if (isRequestHintAppearred) {
                appAnalytics.addParam(AnalyticsEvent.MOBILE_MANUAL_ENTERED.NAME, true)
                AppAnalytics.create(AnalyticsEvent.MOBILE_MANUAL_ENTERED.NAME)
                    .addUserDetails()
                    .addBasicParam()
                    .push()
            }
        }
    }

    private fun requestHint() {
        if (!isRequestHintAppearred) {
            isRequestHintAppearred = true
            val hintRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .setEmailAddressIdentifierSupported(false)
                .build()
            val options = CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build()
            val pendingIntent = Credentials.getClient(AppObjectController.joshApplication, options)
                .getHintPickerIntent(hintRequest)
            startIntentSenderForResult(pendingIntent.intentSender, RC_HINT, null, 0, 0, 0, null)
        }
    }

    private fun getTextView(text: String): TextView {
        val textView = TextView(applicationContext)
        textView.setTextColor(ContextCompat.getColor(applicationContext, R.color.dark_grey))
        TextViewCompat.setTextAppearance(
            textView,
            R.style.TextAppearance_JoshTypography_Body_Text_Small_Semi_Bold
        )
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(
                    applicationContext,
                    R.drawable.ic_small_tick,
                    R.color.success
                ),
                18
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(3, 2, 50, 2)
        textView.setLineSpacing(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.0f,
                applicationContext.resources.displayMetrics
            ), 1.0f
        )
        return textView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val credential: Credential? =
                data?.getParcelableExtra(Credential.EXTRA_KEY)
            binding.mobileEt.setText(
                credential?.id?.replaceFirst(
                    prefix,
                    EMPTY
                )
            )
            if (credential?.id.isNullOrBlank().not())
                appAnalytics.addParam(AnalyticsEvent.MOBILE_AUTOMATICALLY_ENTERED.NAME, true)

        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    fun startPayment() {
        viewModel.testId.postValue(testId)
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        val defaultRegion: String = getDefaultCountryIso(applicationContext)
        appAnalytics.addParam(AnalyticsEvent.COUNTRY_ISO_CODE.NAME, defaultRegion)

        when {
            getPhoneNumber().isBlank() -> {
                when {
                    binding.mobileEt.text.isNullOrEmpty() -> {
                        if (isRequestHintAppearred) {
                            showToast(getString(R.string.please_enter_valid_number))
                        }
                        requestHint()
                        return
                    }
                    isValidFullNumber(
                        prefix,
                        binding.mobileEt.text.toString()
                    ).not() -> {
                        showToast(getString(R.string.please_enter_valid_number))
                        return
                    }
                    isFromNewFreeTrial -> {
                        loginStartFreeTrial = true
                        MixPanelTracker.publishEvent(MixPanelEvent.LOGIN_START_FREE_TRIAL).push()
                        showPopup()
                        return
                    }
                    viewModel.getCourseDiscountedAmount() < 1 -> {
                        viewModel.createFreeOrder(
                            viewModel.getPaymentTestId(),
                            binding.mobileEt.text.toString()
                        )
                        return
                    }
                    prefix == "+91" && viewModel.getCourseDiscountedAmount() >= 1 ->
                        paymentManager.createOrder(
                            binding.mobileEt.text.toString(),
                            viewModel.getEncryptedText(),
                            viewModel.getPaymentTestId()
                        )
                    else ->
                        uiHandler.post {
                            showChatNPayDialog()
                        }
                }
            }
            isFromNewFreeTrial -> {
                loginStartFreeTrial = true
                MixPanelTracker.publishEvent(MixPanelEvent.LOGIN_START_FREE_TRIAL).push()
                showPopup()
                return
            }
            viewModel.getCourseDiscountedAmount() < 1 -> viewModel.createFreeOrder(
                viewModel.getPaymentTestId(),
                getPhoneNumber()
            )
            else -> paymentManager.createOrder(
                getPhoneNumber(),
                viewModel.getEncryptedText(),
                viewModel.getPaymentTestId()
            )
        }

        if (!loginStartFreeTrial) {
            MixPanelTracker.publishEvent(MixPanelEvent.PAYMENT_STARTED)
                .addParam(ParamKeys.TEST_ID, viewModel.getPaymentTestId())
                .addParam(ParamKeys.COURSE_NAME, viewModel.getCourseName())
                .addParam(ParamKeys.COURSE_PRICE, viewModel.getCourseActualAmount())
                .addParam(
                    ParamKeys.IS_COUPON_APPLIED,
                    viewModel.responsePaymentSummary.value?.couponDetails?.isPromoCode
                )
                .addParam(ParamKeys.AMOUNT_PAID, viewModel.getCourseDiscountedAmount())
                .push()
        }
    }

    private fun showPopup() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.freetrial_alert_dialog, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        val width = AppObjectController.screenWidth * .9
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        alertDialog.show()
        alertDialog.window?.setLayout(width.toInt(), height)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var popUpText = " "
        if (isHundredPointsActive && testId == ENGLISH_FREE_TRIAL_1D_TEST_ID || testId == ENGLISH_COURSE_TEST_ID) {
            popUpText = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.FREE_TRIAL_POPUP_HUNDRED_POINTS_TEXT + testId)
                .replace("\\n", "\n")
        } else {
            popUpText =
                if (viewModel.abTestRepository.isVariantActive(VariantKeys.ICP_ENABLED) && testId == ENGLISH_FREE_TRIAL_1D_TEST_ID) {
                    getString(R.string.free_trial_popup_for_icp)
                } else {
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_POPUP_BODY_TEXT + testId)
                        .replace("\\n", "\n")
                }
        }
        dialogView.findViewById<TextView>(R.id.e_g_motivat).text = popUpText

        dialogView.findViewById<TextView>(R.id.add_a_topic).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.FREE_TRIAL_POPUP_TITLE_TEXT + testId)

        dialogView.findViewById<TextView>(R.id.yes).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.FREE_TRIAL_POPUP_YES_BUTTON_TEXT + testId)

        dialogView.findViewById<MaterialTextView>(R.id.yes).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.JI_HAAN).push()
            PrefManager.put(FREE_TRIAL_TEST_ID, testId, false)
            val mobileNumber =
                if (getPhoneNumber().isBlank()) binding.mobileEt.text.toString() else getPhoneNumber()
            viewModel.createFreeOrder(
                viewModel.getPaymentTestId(),
                mobileNumber
            )
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialTextView>(R.id.cancel).setOnClickListener {
            alertDialog.dismiss()
            this.finish()
        }

    }

    fun clearText(v:View) {
        binding.mobileEt.setText(EMPTY)
        appAnalytics.addParam(AnalyticsEvent.MOBILE_NUMBER_CLEARED.NAME, true)
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onBackPressed() {
        backPressMutex.onMultipleBackPress {
            val backPressHandled = paymentManager.getJuspayBackPress()
            if (!backPressHandled) {
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun showPaymentProcessingFragment() {
        binding.container.visibility = View.GONE
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
        try {
            viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.parent_Container,
                PaymentFailedDialogNew.newInstance(paymentManager),
                "Payment Failed"
            )
        }
    }

    private fun showChatNPayDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                ChatNPayDialogFragment.newInstance(),
                "Chat N Pay"
            )
            .commitAllowingStateLoss()
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
            viewModel.getCourseDiscountedAmount().toString()
        )
        this@PaymentSummaryActivity.finish()
    }

    fun showPrivacyPolicyDialog(v:View) {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        showWebViewDialog(url)
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, SignUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FLOW_FROM, "payment journey")
        }
        startActivity(intent)
        val broadcastIntent = Intent().apply {
            action = CALLING_SERVICE_ACTION
            putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
        }
        LocalBroadcastManager.getInstance(this@PaymentSummaryActivity)
            .sendBroadcast(broadcastIntent)
        this.finish()
    }

    override fun onWarmUpEnded(error: String?) {

    }

    private fun onPaymentSuccess() {
        val freeTrialTestId = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID)
        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
            PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
            if (is100PointsObtained) {
                viewModel.saveImpression(POINTS_100_OBTAINED_ENGLISH_COURSE_BOUGHT)
                viewModel.postGoal(
                    GoalKeys.HUNDRED_POINTS_COURSE_BOUGHT.NAME,
                    CampaignKeys.HUNDRED_POINTS.NAME
                )
            }
        }
        appAnalytics.addParam(AnalyticsEvent.PAYMENT_COMPLETED.NAME, true)
        logPaymentStatusAnalyticsEvents(AnalyticsEvent.SUCCESS_PARAM.NAME)
        viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        NotificationUtils(applicationContext).removeAllScheduledNotification()
        //viewModel.updateSubscriptionStatus()
        if (PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).isBlank())
            PrefManager.put(
                PAYMENT_MOBILE_NUMBER,
                prefix.plus(SINGLE_SPACE).plus(binding.mobileEt.text)
            )

        viewModel.saveBranchPaymentLog(
            paymentManager.getJustPayOrderId(),
            BigDecimal(paymentManager.getAmount()),
            testId = Integer.parseInt(freeTrialTestId),
            courseName = "English Course"
        )
        MarketingAnalytics.coursePurchased(
            BigDecimal(paymentManager.getAmount()),
            true,
            testId = freeTrialTestId,
            courseName = "English Course",
            juspayPaymentId = paymentManager.getJustPayOrderId()
        )

        uiHandler.post {
            PrefManager.put(IS_PAYMENT_DONE, true)
        }

        uiHandler.postDelayed({
            navigateToStartCourseActivity(true)
        }, 1000 * 2L)
    }

    override fun onProcessStart() {
        viewModel.savePaymentImpression(PROCEED_PAYMENT_CLICK, "PAYMENT_SUMMARY")
        showProgressBar()
    }

    override fun onProcessStop() {
        viewModel.savePaymentImpression(GATEWAY_INITIALISED, "PAYMENT_SUMMARY")
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
            replace(R.id.parent_Container, fragment, "Payment Processing")
            disallowAddToBackStack()
        }
    }

    override fun onEvent(data: JSONObject?) {
        data?.let {
            viewModel.logPaymentEvent(data)
        }
    }

    private fun showPendingDialog() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentPendingFragment()
            replace(R.id.parent_Container, fragment, "Payment Pending")
            disallowAddToBackStack()
        }
    }
}
