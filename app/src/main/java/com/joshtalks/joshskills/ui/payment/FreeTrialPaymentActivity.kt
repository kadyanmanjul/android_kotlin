package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_PAYMENT_DONE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.getPhoneNumber
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityFreeTrialPaymentBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

const val FREE_TRIAL_PAYMENT_TEST_ID = "102"

class FreeTrialPaymentActivity : CoreJoshActivity(),
    PaymentResultListener {

    private lateinit var binding: ActivityFreeTrialPaymentBinding
    private val viewModel: FreeTrialPaymentViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialPaymentViewModel::class.java)
    }
    private var razorpayOrderId = EMPTY
    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    var index = 0
    var expiredTime: Long = -1
    var buttonText = mutableListOf<String>()
    var headingText = mutableListOf<String>()
    private var countdownTimerBack: CountdownTimerBack? = null

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

        setObservers()
        setListeners()
        viewModel.getPaymentDetails(testId.toInt())
    }

    private fun freeTrialTimerInit(expireTime: String?) {
        if (expireTime != null && expireTime.toDouble().toLong() > 0) {
            binding.freeTrialTimer.visibility = View.VISIBLE
            startTimer((expireTime.toDouble().toLong() - System.currentTimeMillis().div(1000)).times(1000))
        } else {
            binding.freeTrialTimer.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.englishCard.setOnClickListener {
            index = 0
            binding.subscriptionCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.white
                )
            )
            binding.subscriptionCard.setStrokeColor(ContextCompat.getColor(this, R.color.white))
            //binding.title1.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            //binding.title2.setTextColor(ContextCompat.getColor(this, R.color.black))
            binding.englishCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary_alfa_6
                )
            )
            binding.englishCard.setStrokeColor(ContextCompat.getColor(this, R.color.colorPrimary))
            binding.materialTextView.text = buttonText.get(index)
            binding.txtLabelHeading.text = headingText.get(index)
            binding.seeCourseList.visibility = View.GONE

        }
        binding.subscriptionCard.setOnClickListener {
            index = 1

            binding.englishCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.white
                )
            )
            binding.englishCard.setStrokeColor(ContextCompat.getColor(this, R.color.white))

            //binding.title1.setTextColor(ContextCompat.getColor(this, R.color.black))
            //binding.title2.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))

            binding.subscriptionCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary_alfa_6
                )
            )
            binding.subscriptionCard.setStrokeColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary
                )
            )
            binding.materialTextView.text = buttonText.get(index)
            binding.txtLabelHeading.text = headingText.get(index)
            binding.seeCourseList.visibility = View.VISIBLE
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
                        data1.heading?.let { it1 -> headingText.add(it1) }
                        binding.materialTextView.text = buttonText.get(index)
                        binding.txtLabelHeading.text = headingText.get(index)
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
                        data2.heading?.let { it1 -> headingText.add(it1) }
                        binding.title2.text = data2.courseHeading
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
                }
                //TODO condition
                //if(it.expireTime){
                if (true) {
                    binding.freeTrialTimer.text = getString(R.string.free_trial_end_in)
                    //start a timer
                } else {
                    binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
                }
                freeTrialTimerInit(it.expireTime)

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
            options.put(
                "description",
                viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName + "_app"
            )
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
            testId.toInt(),
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
        // isBackPressDisabled = true
        razorpayOrderId.verifyPayment()
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

    }

}
