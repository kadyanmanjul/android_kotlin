package com.joshtalks.joshskills.ui.callWithExpert.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.CourseData
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.razorpay.Checkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.math.BigDecimal

/**
This class is responsible to process payment for wallet recharge payment.
 */

class WalletRechargePaymentManager private constructor(
    private val activity: AppCompatActivity,
    private var selectedAmount: Amount,
    private val viewModelScope: CoroutineScope,
    private var paymentStatusListener: PaymentStatusListener? = null,
    private var navController: NavController? = null
) {

    private val expertListRepo by lazy {
        ExpertListRepo()
    }

    private val signupNetwork by lazy {
        AppObjectController.signUpNetworkService
    }

    private lateinit var courseData: CourseData

    private lateinit var razorpayOrderId: String

    fun startPayment() {
        paymentStatusListener?.onWarmUpStarted()
        getPaymentData()

    }

    private fun getPaymentData() {
        val data = HashMap<String, Any>()
        data["test_id"] = selectedAmount.id
        data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID, false)
        data["code"] = ""
        if (Mentor.getInstance().getId().isNotEmpty()) {
            data["mentor_id"] = Mentor.getInstance().getId()
        }
        viewModelScope.launch {
            try {
                val response = signupNetwork.getFreeTrialPaymentData(data)
                if (response.isSuccessful && response.body() != null) {
                    courseData = response.body()!!.courseData!![0]
                    getOrderDetails(courseData)
                } else {
                    throwError()
                }
            } catch (e: Exception) {
                throwError()
            }
        }
    }

    private fun getOrderDetails(courseData: CourseData) {
        viewModelScope.launch {
            try {
                val data = mutableMapOf(
                    "encrypted_text" to (courseData.encryptedText ?: ""),
                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                    "mobile" to getPhoneNumberOrDefault(),
                    "test_id" to courseData.testId,
                    "mentor_id" to Mentor.getInstance().getId(),
                    "is_micro_payment" to true.toString()
                )

                val orderDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                Log.e("sagar", "getOrderDetails: ${orderDetailsResponse.code()}")
                if (orderDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = orderDetailsResponse.body()!!
                    startPaymentGateway(response)
                    MarketingAnalytics.initPurchaseEvent(data, response)
                } else {
                    throwError()
                }
            } catch (e: Exception) {
                throwError()
            }
        }
    }

    fun startPaymentGateway(orderDetails: OrderDetailResponse) {
        val checkout = Checkout()
        checkout.setImage(R.mipmap.ic_launcher)
        checkout.setKeyID(orderDetails.razorpayKeyId)
        try {
            val preFill = JSONObject()

            if (User.getInstance().email.isNullOrEmpty().not()) {
                preFill.put("email", User.getInstance().email)
            } else {
                preFill.put("email", Utils.getUserPrimaryEmail(activity.applicationContext))
            }
            //preFill.put("contact", "9999999999")

            val options = JSONObject()
            options.put("key", orderDetails.razorpayKeyId)
            options.put("name", "Josh Skills")
            try {
                options.put(
                    "description",
                    courseData.courseName + "_app"
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            options.put("order_id", orderDetails.razorpayOrderId)
            options.put("currency", orderDetails.currency)
            options.put("amount", orderDetails.amount * 100)
            options.put("prefill", preFill)
            paymentStatusListener?.onWarmUpEnded()
            checkout.open(activity, options)
            razorpayOrderId = orderDetails.razorpayOrderId
        } catch (e: Exception) {
            e.printStackTrace()
            paymentStatusListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
        }
    }

    private fun verifyPayment() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf("razorpay_order_id" to razorpayOrderId)
                AppObjectController.commonNetworkService.verifyPaymentWithResponse(data)
//                paymentStatusListener?.onPaymentFinished(isPaymentSuccessful)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showErrorToast() {
        showToast("Something Went Wrong, Please Try Again")
    }

    fun onPaymentSuccess(status: String?) {
        Log.d(TAG, "onPaymentSuccess: and status => $status")
//        onPaymentSuccess: and status => pay_K9qXHKpQIrvIqT
//        showToast("Payment Successful")
        navController?.navigate(R.id.paymentProcessingFragment)
        verifyPayment()
        viewModelScope.launch {
            delay(5000)
            onPaymentFinished(true)
        }
        updateWalletBalance()
    }

    fun updateWalletBalance(){
        expertListRepo.updateWalletBalance()
    }

    fun onPaymentFailed(status: Int, message: String?) {
        // TODO: Do Network Call\
        Log.d(TAG, "onPaymentFailed and status => $status and message => $message")
//      onPaymentFailed and status => 0 and message => {"error":{"code":"BAD_REQUEST_ERROR","description":"Payment processing cancelled by user","source":"customer","step":"payment_authentication","reason":"payment_cancelled","metadata":{"payment_id":"pay_K9qYbitGKSkfO4"}}}
        if (status != 0) {
            // payment not cancelled by user but failed.
            showToast("Payment Failed... Please Try Again")
        }

        verifyPayment()
        viewModelScope.launch {
            delay(5000)
            onPaymentFinished(false)
        }

    }

    fun onPaymentFinished(isPaymentSuccessful: Boolean){
        navController?.let {
            if (isPaymentSuccessful){
                activity.onBackPressed()
            }
        }
        paymentStatusListener?.onPaymentFinished(isPaymentSuccessful)
        // TODO: Show Dialog
    }

    fun throwError() {
        paymentStatusListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
    }

    /**
    Builder for Wallet Payment Manager.
     */

    data class Builder(
        var activity: AppCompatActivity? = null,
        var selectedAmount: Amount? = null,
        var coroutineScope: CoroutineScope? = null,
        var paymentStatusListener: PaymentStatusListener? = null,
        var navController: NavController? = null
    ) {

        fun setActivity(activity: AppCompatActivity) = apply { this.activity = activity }

        fun setSelectedAmount(selectedAmount: Amount) =
            apply { this.selectedAmount = selectedAmount }

        fun setCoroutineScope(coroutineScope: CoroutineScope) =
            apply { this.coroutineScope = coroutineScope }

        fun setPaymentListener(paymentStatusListener: PaymentStatusListener) =
            apply { this.paymentStatusListener = paymentStatusListener }

        fun setNavController(navController: NavController) =
            apply { this.navController = navController }

        fun build(): WalletRechargePaymentManager {
            return WalletRechargePaymentManager(
                activity!!,
                selectedAmount!!,
                coroutineScope!!,
                paymentStatusListener,
                navController
            )
        }

    }

    companion object {
        const val TAG = "WalletPaymentManager"
    }

}

interface PaymentStatusListener {
    fun onWarmUpStarted()
    fun onWarmUpEnded(error: String? = null)
    fun onPaymentFinished(isPaymentSuccessful: Boolean)
}