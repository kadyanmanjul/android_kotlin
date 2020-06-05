package com.joshtalks.joshskills.ui.payment.order_summary

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.facebook.appevents.AppEventsConstants
import com.flurry.android.FlurryAgent
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityPaymentSummaryBinding
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.payment.ChatNPayDialogFragment
import com.joshtalks.joshskills.ui.payment.PaymentFailedDialogFragment
import com.joshtalks.joshskills.ui.payment.PaymentProcessingFragment
import com.joshtalks.joshskills.ui.payment.PaymentSuccessFragment
import com.joshtalks.joshskills.ui.signup.DEFAULT_COUNTRY_CODE
import com.joshtalks.joshskills.ui.signup.RC_HINT
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.sinch.verification.PhoneNumberUtils
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.CurrencyType
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt

const val TRANSACTION_ID = "TRANSACTION_ID"

class PaymentSummaryActivity : CoreJoshActivity(),
    PaymentResultListener {
    private lateinit var binding: ActivityPaymentSummaryBinding
    private var testId: String = EMPTY
    private val uiHandler = Handler(Looper.getMainLooper())
    lateinit var multiLineLL: LinearLayout
    lateinit var typefaceSpan: Typeface
    private lateinit var viewModel: PaymentSummaryViewModel
    private var isEcommereceEventFire = true
    private lateinit var appAnalytics: AppAnalytics

    // TODO (Later)--> payment failed
    private var npsShow = false
    private var isBackPressDisabled = false
    private var isRequestHintAppearred = false
    private var razorpayOrderId = EMPTY

    companion object {
        fun startPaymentSummaryActivity(
            activity: Activity,
            testId: String
        ) {
            Intent(activity, PaymentSummaryActivity::class.java).apply {
                putExtra(TEST_ID_PAYMENT, testId)
            }.run {
                activity.startActivity(this)
            }
        }

        const val TEST_ID_PAYMENT = "test_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        Checkout.preload(application)
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment_summary)
        binding.lifecycleOwner = this
        binding.handler = this
        typefaceSpan = ResourcesCompat.getFont(applicationContext, R.font.poppins)!!
        initToolbarView()
        initViewModel()
        subscribeObservers()
        initCountryCode()
    }

    private fun initToolbarView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.order_summary)
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            appAnalytics.addParam(AnalyticsEvent.BACK_PRESSED.NAME, true)
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            this.finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
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

        viewModel.responsePaymentSummary.observe(this, androidx.lifecycle.Observer {
            val stringListName = it.courseName.split(" with ")
            binding.courseName.text = stringListName.get(0)
            binding.tutorName.text = "with ".plus(it.teacherName)
            val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
            val enrollUser = df.format(it.totalEnrolled)
            binding.enrolled.text = enrollUser.plus("+")
            binding.enrolled.text = enrollUser.plus("+")
            binding.enrolled.setTypeface(binding.enrolled.typeface, Typeface.BOLD)
            binding.rating.text = it.rating.toString()
            appAnalytics.addParam(AnalyticsEvent.COURSE_NAME.NAME, it.courseName)
            appAnalytics.addParam(AnalyticsEvent.COURSE_PRICE.NAME, it.discountAmount)

            val multi = MultiTransformation(
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(applicationContext)
                .load(it.imageUrl)
                .apply(RequestOptions.bitmapTransform(multi))
                .override(Target.SIZE_ORIGINAL)
                .into(binding.profileImage)
            binding.txtPrice.text =
                "₹ ${String.format("%.2f", it.amount)}"
            binding.materialButton.text =
                "Pay ₹ ${it.discountAmount.roundToInt()}"
            multiLineLL = binding.multiLineLl

            it.features?.let {
                val stringList = it.split(",")
                if (stringList.isNullOrEmpty().not())
                    stringList.forEach {
                        if (it.isEmpty().not()) multiLineLL.addView(getTextView(it))
                    }
            }
            if (it.couponDetails.title.isEmpty().not()) {
                binding.textView1.text = it.couponDetails.name
                binding.tvTip.text = it.couponDetails.title
                binding.tvTipValid.text = it.couponDetails.validity
                binding.tvTipOff.text = it.couponDetails.header
                binding.group1.visibility = View.GONE
                appAnalytics.addParam(AnalyticsEvent.SPECIAL_DISCOUNT.NAME, it.couponDetails.title)
                binding.badeBhaiyaTipContainer.visibility = View.VISIBLE
                binding.badeBhaiyaTipContainer.setOnClickListener {
                    appAnalytics.addParam(AnalyticsEvent.HAVE_COUPON_CODE.NAME, true)
                    binding.badeBhaiyaTipContainer.visibility = View.INVISIBLE
                    binding.txtPrice.text =
                        "₹ ${String.format("%.2f", viewModel.getCourseAmount())}"

                    binding.tipUsedMsg.text = SpannableStringBuilder(
                        getString(
                            R.string.tip_used_info,
                            viewModel.getDiscount().toString()
                        )
                    )
                    binding.tipUsedMsg.visibility = View.VISIBLE
                }
            }
        })
        if (viewModel.hasAnyUserDetails) {
            binding.group1.visibility = View.GONE
        }
        viewModel.mPaymentDetailsResponse.observe(this, androidx.lifecycle.Observer {
            initializeRazorpayPayment(it)
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(PaymentSummaryViewModel::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                data["test_id"] = testId
                data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID)

                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                }
                // TODO later for coupons
                if (false) {
                    data["coupon"] = "testcoupon"
                }
                viewModel.getPaymentSummaryDetails(data)
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    private fun initializeRazorpayPayment(response: OrderDetailResponse) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.progressBar.visibility = View.VISIBLE
            val checkout = Checkout()
            checkout.setImage(R.mipmap.ic_launcher)
            checkout.setKeyID(response.razorpayKeyId)
            try {
                val preFill = JSONObject()

                if (!viewModel.hasAnyUserDetails && User.getInstance().email.isNotBlank())
                    preFill.put("email", User.getInstance().email)
                else
                    preFill.put("email", Utils.getUserPrimaryEmail(applicationContext))

                if (!viewModel.hasAnyUserDetails)
                    preFill.put("contact", binding.mobileEt.text.toString())
                else if (User.getInstance().phoneNumber.isNotBlank())
                    preFill.put("contact", User.getInstance().phoneNumber)
                else if (PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).isNotBlank())
                    preFill.put("contact", PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER))
                else
                    preFill.put("contact", "9999999999")
                val options = JSONObject()
                options.put("key", response.razorpayKeyId)
                options.put("name", "Josh Skills")
                options.put("description", viewModel.getCourseName() + "_app")
                options.put("order_id", response.razorpayOrderId)
                options.put("currency", response.currency)
                options.put("amount", response.amount * 100)
                options.put("prefill", preFill)
                checkout.open(this@PaymentSummaryActivity, options)
                razorpayOrderId = response.razorpayOrderId
                binding.progressBar.visibility = View.GONE
                appAnalytics
                    .addParam("razor id", razorpayOrderId)
                    .addParam(AnalyticsEvent.TRANSACTION_ID.NAME, response.joshtalksOrderId)

            } catch (e: Exception) {
                e.printStackTrace()
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
        binding.countryCodePicker.setCountryForNameCode(DEFAULT_COUNTRY_CODE)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        binding.mobileEt.prefix =
            binding.countryCodePicker.defaultCountryCodeWithPlus

        binding.countryCodePicker.setOnCountryChangeListener {
            binding.mobileEt.prefix =
                binding.countryCodePicker.selectedCountryCodeWithPlus
        }
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
        textView.typeface = typefaceSpan
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(
                    applicationContext,
                    R.drawable.ic_small_tick,
                    R.color.green
                ),
                22
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(2, 2, 0, 2)
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
                    binding.mobileEt.prefix,
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
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }
        val defaultRegion: String = PhoneNumberUtils.getDefaultCountryIso(applicationContext)
        appAnalytics.addParam(AnalyticsEvent.COUNTRY_ISO_CODE.NAME, defaultRegion)
        if (defaultRegion == "IN") {
            if (viewModel.hasAnyUserDetails.not()) {
                if (binding.mobileEt.text.isNullOrEmpty()) {
                    if (isRequestHintAppearred) {
                        binding.inputLayoutPassword.error = "Please enter your phone number first"
                        binding.inputLayoutPassword.isErrorEnabled = true
                    }
                    requestHint()
                    return
                } else if (isValidFullNumber(
                        binding.mobileEt.prefix,
                        binding.mobileEt.text.toString()
                    ).not()
                ) {
                    binding.inputLayoutPassword.error = "Please enter valid phone number"
                    binding.inputLayoutPassword.isErrorEnabled = true
                    return
                } else viewModel.getOrderDetails(testId, binding.mobileEt.text.toString())
                binding.inputLayoutPassword.isErrorEnabled = false
            } else if (User.getInstance().phoneNumber.isNotBlank())
                viewModel.getOrderDetails(testId, User.getInstance().phoneNumber)
            else
                viewModel.getOrderDetails(testId, PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER))
        } else
            uiHandler.post {
                showChatNPayDialog()
            }
    }

    fun clearText() {
        binding.mobileEt.setText("")
        appAnalytics.addParam(AnalyticsEvent.MOBILE_NUMBER_CLEARED.NAME, true)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        appAnalytics.addParam(AnalyticsEvent.PAYMENT_FAILED.NAME, p1)
        isBackPressDisabled = true
        uiHandler.post {
            showPaymentFailedDialog()
        }
        NPSEventModel.setCurrentNPA(NPSEvent.PAYMENT_FAILED)
    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        appAnalytics.addParam(AnalyticsEvent.PAYMENT_COMPLETED.NAME, true)
        isBackPressDisabled = true
        razorpayOrderId.verifyPayment()
        NPSEventModel.setCurrentNPA(
            NPSEvent.PAYMENT_SUCCESS
        )
        PrefManager.put(
            PAYMENT_MOBILE_NUMBER,
            binding.mobileEt.prefix.plus(SINGLE_SPACE).plus(binding.mobileEt.text.toString())
        )
        if (isEcommereceEventFire && (viewModel.mPaymentDetailsResponse.value?.amount!! > 0) && razorpayPaymentId.isNotEmpty() && testId.isNotEmpty()) {
            isEcommereceEventFire = false
            addECommerceEvent(razorpayPaymentId)
        }

        uiHandler.post {
            showPaymentProcessingFragment()
        }

        uiHandler.postDelayed({
            showPaymentSuccessfulFragment()
        }, 1000 * 5)

        uiHandler.postDelayed({
            StartCourseActivity.openStartCourseActivity(
                this,
                viewModel.responsePaymentSummary.value?.courseName ?: "Course",
                viewModel.responsePaymentSummary.value?.teacherName ?: EMPTY,
                viewModel.responsePaymentSummary.value?.imageUrl ?: EMPTY,
                viewModel.mPaymentDetailsResponse.value?.joshtalksOrderId ?: 0
            )
            this@PaymentSummaryActivity.finish()
        }, 1000 * 8)

        FlurryAgent.UserProperties.set(FlurryAgent.UserProperties.PROPERTY_PURCHASER, "true")
    }

    private fun addECommerceEvent(razorpayPaymentId: String) {
        WorkMangerAdmin.newCourseScreenEventWorker(
            viewModel.getCourseName(),
            testId,
            buyCourse = true
        )

        AppObjectController.firebaseAnalytics.resetAnalyticsData()
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, testId)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, viewModel.getCourseName())
        bundle.putDouble(
            FirebaseAnalytics.Param.PRICE, viewModel.getCourseAmount()
        )
        bundle.putString(FirebaseAnalytics.Param.TRANSACTION_ID, razorpayPaymentId)
        bundle.putString(FirebaseAnalytics.Param.CURRENCY, "INR")
        AppObjectController.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)

        val extras: HashMap<String, String> = HashMap()
        extras["test_id"] = testId
        extras["payment_id"] = razorpayPaymentId
        extras["currency"] = CurrencyType.INR.name
        extras["amount"] = viewModel.getCourseAmount().toString()
        extras["course_name"] = viewModel.getCourseName()
        BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.PURCHASE, extras)

        try {
            if (viewModel.getCourseAmount() <= 0) {
                return
            }
            AppObjectController.facebookEventLogger.flush()
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, testId)
            }
            AppObjectController.facebookEventLogger.logPurchase(
                viewModel.getCourseAmount().toBigDecimal(),
                Currency.getInstance(viewModel.getCurrency().trim()),
                params
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            Crashlytics.logException(ex)
        }
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

    override fun onBackPressed() {
        if (!isBackPressDisabled)
            super.onBackPressed()
    }

    override fun onDestroy() {
        appAnalytics.push()
        super.onDestroy()
        Checkout.clearUserData(applicationContext)
        uiHandler.removeCallbacksAndMessages(null)
        AppObjectController.facebookEventLogger.flush()
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
            .commit()
    }

    private fun showPaymentFailedDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentFailedDialogFragment.newInstance(
                    viewModel.mPaymentDetailsResponse.value?.joshtalksOrderId ?: 0
                ),
                "Payment Success"
            )
            .commit()
    }

    private fun showPaymentSuccessfulFragment() {
        binding.container.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentSuccessFragment.newInstance(),
                "Payment Success"
            )
            .commit()
    }

    private fun showChatNPayDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                ChatNPayDialogFragment.newInstance(),
                "Chat N Pay"
            )
            .commit()
    }
}
