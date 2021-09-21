package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_PAYMENT_DONE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.getPhoneNumber
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityFreeTrialPaymentBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
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

        setObservers()
        setListeners()
        viewModel.getPaymentDetails(testId.toInt())
    }

    private fun setListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setObservers() {
        viewModel.paymentDetailsLiveData.observe(this) {
            binding.txtLabelHeading.text = it.heading
            for (i in it.subHeadings.indices) {
                when (i) {
                    0 -> {
                        binding.txtPointer1.text = it.subHeadings[i]
                        binding.txtPointer1.visibility = View.VISIBLE
                    }
                    1 -> {
                        binding.txtPointer2.text = it.subHeadings[i]
                        binding.txtPointer2.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.txtPointer3.text = it.subHeadings[i]
                        binding.txtPointer3.visibility = View.VISIBLE
                    }
                    3 -> {
                        binding.txtPointer4.text = it.subHeadings[i]
                        binding.txtPointer4.visibility = View.VISIBLE
                    }
                    4 -> {
                        binding.txtPointer5.text = it.subHeadings[i]
                        binding.txtPointer5.visibility = View.VISIBLE
                    }
                    5 -> {
                        binding.txtPointer6.text = it.subHeadings[i]
                        binding.txtPointer6.visibility = View.VISIBLE
                    }
                    6 -> {
                        binding.txtPointer7.text = it.subHeadings[i]
                        binding.txtPointer7.visibility = View.VISIBLE
                    }
                    7 -> {
                        binding.txtPointer8.text = it.subHeadings[i]
                        binding.txtPointer8.visibility = View.VISIBLE
                    }
                    8 -> {
                        binding.txtPointer9.text = it.subHeadings[i]
                        binding.txtPointer9.visibility = View.VISIBLE
                    }
                    9 -> {
                        binding.txtPointer10.text = it.subHeadings[i]
                        binding.txtPointer10.visibility = View.VISIBLE
                    }
                    10 -> {
                        binding.txtPointer11.text = it.subHeadings[i]
                        binding.txtPointer11.visibility = View.VISIBLE
                    }
                }
            }
            binding.txtCurrency.text = it.discount[0].toString()
            binding.txtFinalPrice.text = it.discount.substring(1)
            binding.txtOgPrice.text = getString(R.string.price, it.actualAmount)
            binding.txtOgPrice.paintFlags =
                binding.txtOgPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.txtSaving.text = getString(R.string.savings, it.savings)
            binding.courseRating.rating = it.rating.toFloat()
            binding.txtTotalReviews.text = "(" + String.format("%,d", it.ratingsCount) + ")"
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
            options.put("description", viewModel.paymentDetailsLiveData.value?.courseName + "_app")
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

        viewModel.getOrderDetails(testId.toInt(), phoneNumber)
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
            viewModel.paymentDetailsLiveData.value?.courseName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.teacherName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.imageUrl ?: EMPTY,
            viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
        )
        this.finish()
    }

    companion object {

        fun startFreeTrialPaymentActivity(activity: Activity, testId: String) {
            Intent(activity, FreeTrialPaymentActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_top)
            }
        }

    }

}
