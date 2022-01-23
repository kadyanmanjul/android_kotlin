package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics.logNewPaymentPageOpened
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.databinding.ActivityFreeTrialPaymentBinding
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.voip.CallForceDisconnect
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.android.synthetic.main.fragment_sign_up_profile_for_free_trial.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.math.BigDecimal

const val FREE_TRIAL_PAYMENT_TEST_ID = "102"
const val IS_FAKE_CALL = "is_fake_call"

class FreeTrialPaymentActivity : CoreJoshActivity(),
    PaymentResultListener {

    private lateinit var binding: ActivityFreeTrialPaymentBinding
    private val viewModel: FreeTrialPaymentViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialPaymentViewModel::class.java)
    }
    private var razorpayOrderId = EMPTY
    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    var index = 1
    var expiredTime: Long = -1
    var buttonText = mutableListOf<String>()
    var headingText = mutableListOf<String>()
    private var countdownTimerBack: CountdownTimerBack? = null

    lateinit var connectivityManager : ConnectivityManager
    lateinit var networkInfo : NetworkInfo
    var pdfQuestion: LessonQuestion?= null
    val d2pSyllabusPdfResponseLessonType: MutableLiveData<LessonQuestion> = MutableLiveData()

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
            testId = intent.getStringExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)!!
        }
        if (intent.hasExtra(EXPIRED_TIME)) {
            expiredTime = intent.getLongExtra(EXPIRED_TIME, -1)
        }
        if (intent.hasExtra(IS_FAKE_CALL)) {
            forceDisconnectCall()
            val nameArr = User.getInstance().firstName?.split(" ")
            val firstName = if (nameArr != null) nameArr[0] else EMPTY
            showToast(getString(R.string.feature_locked, firstName), Toast.LENGTH_LONG)
        }

        setObservers()
        setListeners()
        viewModel.getPaymentDetails(testId.toInt())
        logNewPaymentPageOpened()

//        viewModel.getD2pSyllabusPdfData()
//        viewModel.d2pSyllabusPdfResponse.observe(this, {
//            pdfQuestion = LessonQuestion(pdfList = listOf(PdfType(url = it.SyllabusPdfLink)))
//         //   pdfQuestion!!.pdfList?.get(0)?.let { it1 -> showToast(it1.url) }
//            d2pSyllabusPdfResponseLessonType.postValue(LessonQuestion(pdfList = listOf(PdfType(url = it.SyllabusPdfLink))))
//        })
//
//        d2pSyllabusPdfResponseLessonType.observe(this, {
//            it.pdfList?.get(0)?.url?.let { it1 -> Log.e("sakshi_hjjjj", it1) }
//        })


        viewModel.getD2pSyllabusPdfData()
            viewModel.d2pSyllabusPdfResponse.observe(this,{
                var str = it.SyllabusPdfLink
                var splitted = str.split(".pdf")
                var first = splitted[0]
                Log.e("sakshi_pdf", first.toString())
                binding.syllabusPdfCard.setOnClickListener {
//                    networkInfo = connectivityManager.activeNetworkInfo!!
//                    if(networkInfo == null){
//                        Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show()
//                    }else{
                        var intent: Intent = Intent(applicationContext, WebActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.putExtra("pdf_url", first + ".pdf")
                        startActivity(intent)
                  //  }
                }
            })




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

    private fun setListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.englishCard.setOnClickListener {
            try {
                index = 0
                binding.subscriptionCard.background =
                    ContextCompat.getDrawable(this, R.drawable.white_rectangle_with_grey_stroke)
                binding.englishCard.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.blue_rectangle_with_blue_bound_stroke
                    )
                binding.materialTextView.text = buttonText.get(index)
                binding.txtLabelHeading.text = headingText.get(index)
                binding.seeCourseList.visibility = View.GONE
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        binding.subscriptionCard.setOnClickListener {
            try {
                index = 1
                binding.subscriptionCard.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.blue_rectangle_with_blue_bound_stroke
                    )
                binding.englishCard.background =
                    ContextCompat.getDrawable(this, R.drawable.white_rectangle_with_grey_stroke)
                binding.materialTextView.text = buttonText.get(index)
                binding.txtLabelHeading.text = headingText.get(index)
                binding.seeCourseList.visibility = View.VISIBLE
                scrollToBottom()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        binding.seeCourseList.setOnClickListener {
            CourseExploreActivity.startCourseExploreActivity(
                this,
                COURSE_EXPLORER_CODE,
                null,
                state = ActivityEnum.FreeTrial,
                isClickable = false
            )
        }
    }

    override fun onStart() {
        super.onStart()
        binding.subscriptionCard.performClick()
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    binding.freeTrialTimer
                    binding.freeTrialTimer.text = getString(
                        R.string.free_trial_end_in,
                        UtilTime.timeFormatted(millis)
                    )
                }
            }

            override fun onTimerFinish() {
                binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
            }
        }
        countdownTimerBack?.startTimer()
    }

    fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setObservers() {
        viewModel.paymentDetailsLiveData.observe(this) {
            try {
                buttonText = mutableListOf<String>()
                headingText = mutableListOf<String>()
                it.subHeadings?.let { list ->
                    for (i in list.indices) {
                        when (i) {
                            0 -> {
                                binding.txtPointer1.text = list[i]
                                binding.txtPointer1.visibility = View.VISIBLE
                            }
                            1 -> {
                                binding.txtPointer2.text = list[i]
                                binding.txtPointer2.visibility = View.VISIBLE
                            }
                            2 -> {
                                binding.txtPointer3.text = list[i]
                                binding.txtPointer3.visibility = View.VISIBLE
                            }
                            3 -> {
                                binding.txtPointer4.text = list[i]
                                binding.txtPointer4.visibility = View.VISIBLE
                            }
                            4 -> {
                                binding.txtPointer5.text = list[i]
                                binding.txtPointer5.visibility = View.VISIBLE
                            }
                            5 -> {
                                binding.txtPointer6.text = list[i]
                                binding.txtPointer6.visibility = View.VISIBLE
                            }
                            6 -> {
                                binding.txtPointer7.text = list[i]
                                binding.txtPointer7.visibility = View.VISIBLE
                            }
                            7 -> {
                                binding.txtPointer8.text = list[i]
                                binding.txtPointer8.visibility = View.VISIBLE
                            }
                            8 -> {
                                binding.txtPointer9.text = list[i]
                                binding.txtPointer9.visibility = View.VISIBLE
                            }
                            9 -> {
                                binding.txtPointer10.text = list[i]
                                binding.txtPointer10.visibility = View.VISIBLE
                            }
                            10 -> {
                                binding.txtPointer11.text = list[i]
                                binding.txtPointer11.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                it.courseData?.let {
                    val data1 = it.get(0)
                    if (data1 != null) {
                        data1.buttonText?.let { it1 -> buttonText.add(it1) }
                        data1.heading.let { it1 -> headingText.add(it1) }

                        binding.title1.text = data1.courseHeading
                        binding.txtCurrency1.text = data1.discount?.get(0).toString()
                        binding.txtFinalPrice1.text = data1.discount?.substring(1)
                        binding.txtOgPrice1.text = getString(R.string.price, data1.actualAmount)
                        binding.txtOgPrice1.paintFlags =
                            binding.txtOgPrice1.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        binding.txtSaving1.text = getString(R.string.savings, data1.savings)
                        binding.courseRating1.rating = data1.rating?.toFloat() ?: 4f
                        binding.txtTotalReviews1.text =
                            "(" + String.format("%,d", data1.ratingsCount) + ")"
                    } else {
                        binding.englishCard.visibility = View.GONE
                    }

                    val data2 = it.get(1)
                    if (data2 != null) {
                        data2.buttonText?.let { it1 -> buttonText.add(it1) }
                        data2.heading.let { it1 -> headingText.add(it1) }
                        binding.title2.text = data2.courseHeading
                        binding.txtCurrency2.text = data2.discount?.get(0).toString()
                        if (data2.perCoursePrice.isNullOrBlank()) {
                            binding.perCourseText.visibility = View.GONE
                        } else {
                            binding.perCourseText.visibility = View.VISIBLE
                            binding.perCourseText.text = data2.perCoursePrice
                        }
                        binding.txtCurrency2.text = data2.discount?.get(0).toString()
                        binding.txtFinalPrice2.text = data2.discount?.substring(1)
                        binding.txtOgPrice2.text = getString(R.string.price, data2.actualAmount)
                        binding.txtOgPrice2.paintFlags =
                            binding.txtOgPrice2.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        binding.txtSaving2.text = getString(R.string.savings, data2.savings)
                        binding.courseRating2.rating = data2.rating?.toFloat() ?: 4f
                        binding.txtTotalReviews2.text =
                            "(" + String.format("%,d", data2.ratingsCount) + ")"
                    } else {
                        binding.subscriptionCard.visibility = View.GONE
                    }
                    try {
                        binding.materialTextView.text = buttonText.get(index)
                        binding.txtLabelHeading.text = headingText.get(index)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                if (it.expireTime != null) {
                    binding.freeTrialTimer.visibility = View.VISIBLE
                    if (it.expireTime.time >= System.currentTimeMillis()) {
                        startTimer(it.expireTime.time - System.currentTimeMillis())
                    } else {
                        binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
                    }
                } else {
                    binding.freeTrialTimer.visibility = View.GONE
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        viewModel.orderDetailsLiveData.observe(this) {
            initializeRazorpayPayment(it)
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            if (isProcessing) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
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
            } catch (ex:Exception){
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

        viewModel.getOrderDetails(
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId ?: testId,
            phoneNumber,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.encryptedText ?: EMPTY
        )
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
        uiHandler.post {
            showPaymentFailedDialog()
        }
    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
            PrefManager.put(IS_DEMO_P2P, false)
        }
        val freeTrialTestId = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID)
        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
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
        StartCourseActivity.openStartCourseActivity(
            this,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.teacherName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.imageUrl ?: EMPTY,
            viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
        )
        this.finish()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
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
                activity.overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_top)
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
    fun openD2pSyllabusPdf(){
//        viewModel.getD2pSyllabusPdfData()
//        viewModel.d2pSyllabusPdfResponse.observe(this,{
//            var str = it.SyllabusPdfLink
//            var splitted = str.split(".pdf")
//            var first = splitted[0]
//            Log.e("sihiya", first + ".pdf")
//        })
//        showProgressBar()
//        var intent : Intent = Intent(applicationContext, WebActivity::class.java)
//        intent.putExtra("pdf_url", "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/media/ULTIMATE_SPOKEN_ENGLISH_COURSE_SYLLABUS_V2.pdf")
//        startActivity(intent)
//       // hideProgressBar()
    }

}
