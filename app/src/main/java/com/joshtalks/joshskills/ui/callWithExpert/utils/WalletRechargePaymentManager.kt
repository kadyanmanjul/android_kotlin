package com.joshtalks.joshskills.ui.callWithExpert.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseData
import com.joshtalks.joshskills.ui.callWithExpert.fragment.RechargeSuccessFragment
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
This class is responsible to process payment for wallet recharge payment.
 */

class WalletRechargePaymentManager private constructor(
    private val activity: AppCompatActivity,
    private var selectedAmount: Amount,
    private val viewModelScope: CoroutineScope,
    private var paymentStatusListener: PaymentStatusListener? = null,
    private var paymentGatewayListener: PaymentGatewayListener? = null,
    private var navController: NavController? = null,
    private var paymentManager: PaymentManager? = null
) {

    private val expertListRepo by lazy {
        ExpertListRepo()
    }

    private val signupNetwork by lazy {
        AppObjectController.signUpNetworkService
    }

    private lateinit var courseData: CourseData

    private lateinit var razorpayOrderId: String

//    fun initializePaymentGateway(){
//        paymentManager?.initializePaymentGateway()
//    }


    fun startPayment() {
        paymentGatewayListener?.onProcessStart()
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
//                    selectedAmount =
//                        Amount(courseData.actualAmount!!.removePrefix("₹").toFloat().toInt(), courseData.testId.toInt(),)
                    paymentManager?.createForWallet(courseData, selectedAmount)
                } else {
                    paymentGatewayListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                paymentGatewayListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
            }
        }
    }

    private fun verifyPayment() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
//                val data = mapOf("razorpay_order_id" to razorpayOrderId)
                AppObjectController.commonNetworkService.verifyPaymentV3(paymentManager?.getJustPayOrderId() ?: "")
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
        verifyPayment()
        viewModelScope.launch {
            delay(5000)
            onPaymentFinished(true)
        }
        updateWalletBalance()
    }

    fun updateWalletBalance() {
        viewModelScope.launch {
            expertListRepo.updateWalletBalance()
        }
    }

    fun onPaymentFailed(status: Int, message: String?) {
        Log.d(TAG, "onPaymentFailed and status => $status and message => $message")
        if (status != 0) {
            // payment not cancelled by user but failed.
            showToast("Payment Failed... Please Try Again")

            // TODO: Verify Payment Maybe Called Later.
//            verifyPayment()
        }

        viewModelScope.launch {
            delay(5000)
            onPaymentFinished(false)
        }

    }

    fun onPaymentFinished(isPaymentSuccessful: Boolean) {
        navController?.let {
            if (isPaymentSuccessful) {
                activity.onBackPressed()
                RechargeSuccessFragment.open(
                    activity.supportFragmentManager,
                    amount = selectedAmount.amount,
                    type = "Wallet"
                )
            }
        }
        paymentGatewayListener?.onPaymentFinished(isPaymentSuccessful)

    }

    /**
    Builder for Wallet Payment Manager.
     */

    data class Builder(
        var activity: AppCompatActivity? = null,
        var selectedAmount: Amount? = null,
        var coroutineScope: CoroutineScope? = null,
        var paymentStatusListener: PaymentStatusListener? = null,
        var paymentGatewayListener: PaymentGatewayListener? = null,
        var navController: NavController? = null,
        var paymentManager: PaymentManager? = null,
    ) {

        fun setActivity(activity: AppCompatActivity) = apply { this.activity = activity }

        fun setSelectedAmount(selectedAmount: Amount) =
            apply { this.selectedAmount = selectedAmount }

        fun setCoroutineScope(coroutineScope: CoroutineScope) =
            apply { this.coroutineScope = coroutineScope }

        fun setPaymentListener(paymentStatusListener: PaymentStatusListener) =
            apply { this.paymentStatusListener = paymentStatusListener }

        fun setPaymentGatewayListener(paymentGatewayListener: PaymentGatewayListener) =
            apply { this.paymentGatewayListener = paymentGatewayListener }

        fun setNavController(navController: NavController) =
            apply { this.navController = navController }

        fun setPaymentManager(paymentManager: PaymentManager) =
            apply { this.paymentManager = paymentManager }

        fun build(): WalletRechargePaymentManager {
            return WalletRechargePaymentManager(
                activity!!,
                selectedAmount!!,
                coroutineScope!!,
                paymentStatusListener,
                paymentGatewayListener,
                navController,
                paymentManager
            )
        }

    }

    companion object {
        const val TAG = "WalletPaymentManager"
        var selectedExpertForCall: ExpertListModel? = null
    }

}

interface PaymentStatusListener {
    fun onPaymentFinished(isPaymentSuccessful: Boolean)
}