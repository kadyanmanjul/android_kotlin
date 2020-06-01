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
import android.text.Spanned
import android.text.style.IconMarginSpan
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCoursePaymentBinding
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.signup.DEFAULT_COUNTRY_CODE
import com.joshtalks.joshskills.ui.signup.RC_HINT
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.CurrencyType
import io.github.inflationx.calligraphy3.TypefaceUtils
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

class PaymentSummaryActivity : CoreJoshActivity(),
    PaymentResultListener {
    private lateinit var binding: ActivityCoursePaymentBinding
    private var testId: String = EMPTY
    private val uiHandler = Handler(Looper.getMainLooper())
    lateinit var multiLineLL: LinearLayout
    lateinit var typefaceSpan: Typeface
    private lateinit var viewModel: OrderSummaryViewModel
    val PHONE_NUMBER_REGEX = Regex(pattern = "^[6789]\\d{9}\$")
    private var isEcommereceEventFire = true
    private var npsShow = true
    private var isRegisteredAlready = false

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
        if (intent.hasExtra(TEST_ID_PAYMENT)) {
            val temp = intent.getStringExtra(TEST_ID_PAYMENT)
            if (temp.isNullOrEmpty()) {
                this.finish()
                return
            }
            this.testId = temp
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_payment)
        binding.lifecycleOwner = this
        typefaceSpan = TypefaceUtils.load(
            assets,
            "fonts/Roboto-Regular.ttf"
        )
        initViewModel()
        subscribeObservers()
        initCountryCode()
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.viewState?.observe(this, androidx.lifecycle.Observer {
            when (it) {
                OrderSummaryViewModel.ViewState.INTERNET_NOT_AVAILABLE -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(getString(R.string.internet_not_available_msz))
                }
                OrderSummaryViewModel.ViewState.ERROR_OCCURED -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
                }
                OrderSummaryViewModel.ViewState.PROCESSING -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        })

        viewModel.responsePaymentSummary.observe(this, androidx.lifecycle.Observer {
            val stringListName = it.name.split(" with ")
            binding.courseName.text = stringListName.get(0)
            binding.tutorName.text = "with ".plus(it.teacherName)
            val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
            val enrollUser = df.format(it.totalEnrolled)
            binding.enrolled.text = enrollUser.plus("+")
            binding.rating.text = it.rating.toString()

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
                "₹ ${String.format("%.2f", it.discountAmount)}"
            binding.materialButton.text =
                "Pay ₹ ${it.discountAmount.roundToInt()}"
            multiLineLL = binding.multiLineLl

            val stringList = it.features.split(",")
            if (stringList.isNullOrEmpty().not())
                stringList.forEach {
                    if (it.isEmpty().not()) multiLineLL.addView(getTextView(it))
                }
            if (it.couponDetails.title.isEmpty().not()) {
                binding.textView1.text = it.couponDetails.name
                binding.tvTip.text = it.couponDetails.title
                binding.tvTipValid.text = it.couponDetails.validity
                binding.tvTipValid.text = it.couponDetails.header
                binding.group1.visibility = View.GONE
                binding.badeBhaiyaTipContainer.visibility = View.VISIBLE
                binding.badeBhaiyaTipContainer.setOnClickListener {
                    binding.badeBhaiyaTipContainer.visibility = View.INVISIBLE
                    binding.tipUsedMsg.visibility = View.VISIBLE
                }
            }
        })
        if (isRegisteredAlready) {
            binding.group1.visibility = View.GONE
        } else requestHint()
        viewModel.mPaymentDetailsResponse.observe(this, androidx.lifecycle.Observer {
            initializeRazorpayPayment(it)
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(OrderSummaryViewModel::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                data["test_id"] = testId
                data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID)

                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                    isRegisteredAlready = true
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
                    .put("email", Utils.getUserPrimaryEmail(applicationContext))
                if (User.getInstance().phoneNumber.isEmpty())
                    preFill.put("contact", binding.mobileEt.text.toString())
                else preFill.put("contact", User.getInstance().phoneNumber)
                val options = JSONObject()
                options.put("key", response.razorpayKeyId)
                options.put("name", "Josh Skills")
                options.put("description", viewModel.getCourseName() + "_app")
                options.put("order_id", response.razorpayOrderId)
                options.put("currency", response.currency)
                options.put("amount", response.amount * 100)
                options.put("prefill", preFill)
                checkout.open(this@PaymentSummaryActivity, options)
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initCountryCode() {
        binding.countryCodePicker.setDefaultCountryUsingNameCode(
            DEFAULT_COUNTRY_CODE
        )
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setCountryForNameCode(DEFAULT_COUNTRY_CODE)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        binding.mobileEt.prefix =
            binding.countryCodePicker.defaultCountryCodeWithPlus

        binding.countryCodePicker.setOnCountryChangeListener {
            binding.mobileEt.prefix =
                binding.countryCodePicker.selectedCountryCodeWithPlus
        }
    }

    private fun requestHint() {
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

    private fun getTextView(text: String): TextView {
        val textView = TextView(applicationContext)
        textView.setTextColor(ContextCompat.getColor(applicationContext, R.color.gray_48))
        textView.typeface = typefaceSpan
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(applicationContext, R.drawable.ic_small_tick),
                22
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(0, 2, 0, 2)
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
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    fun startPayment() {
        if (!isRegisteredAlready && binding.mobileEt.text.isNullOrEmpty()) {
            Toast.makeText(
                AppObjectController.joshApplication,
                getString(R.string.please_enter_your_mobile_number),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        if (!isRegisteredAlready && validationForIndiaOnly() && validPhoneNumber(binding.mobileEt.text.toString()).not()) {
            binding.inputLayoutPassword.error = "Please enter valid phone number"
            binding.inputLayoutPassword.isErrorEnabled = true
            return
        }
        if (!PrefManager.hasKey("mobile_no"))
            PrefManager.put("mobile_no", binding.mobileEt.text.toString())
        binding.inputLayoutPassword.isErrorEnabled = false
        viewModel.getOrderDetails(testId, PrefManager.getStringValue("mobile_no"))
    }

    private fun validPhoneNumber(number: String): Boolean {
        return this.PHONE_NUMBER_REGEX.containsMatchIn(input = number)
    }

    private fun validationForIndiaOnly(): Boolean {
        return binding.mobileEt.prefix.startsWith("+91")
    }

    fun clearText() {
        showToast("Mobile number entered is cleared")
        binding.mobileEt.setText("")
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showToast("Payment Failed")
        // TODO sahil on PaymentError
        if (npsShow) {
            NPSEventModel.setCurrentNPA(NPSEvent.PAYMENT_FAILED)
            showNetPromoterScoreDialog()
            npsShow = false
        }

    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        showToast("Payment Verified")
        razorpayPaymentId.verifyPayment()
        NPSEventModel.setCurrentNPA(
            NPSEvent.PAYMENT_SUCCESS
        )
        if (isEcommereceEventFire && (viewModel.mPaymentDetailsResponse.value?.amount!! > 0) && razorpayPaymentId.isNotEmpty() && testId.isNotEmpty()) {
            isEcommereceEventFire = false
            addECommerceEvent(razorpayPaymentId)
        }

        uiHandler.post {
            // TODO sahil on PaymentSuccess
        }
        uiHandler.postDelayed({
            startActivity(getInboxActivityIntent())
            this@PaymentSummaryActivity.finish()
        }, 1000 * 59)
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

    override fun onDestroy() {
        super.onDestroy()
        Checkout.clearUserData(applicationContext)
        uiHandler.removeCallbacksAndMessages(null)
        AppObjectController.facebookEventLogger.flush()
    }
}
