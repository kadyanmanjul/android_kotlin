package com.joshtalks.joshskills.ui.callWithExpert.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.razorpay.Checkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
    This class is responsible to process payment for wallet recharge payment.
*/

class WalletRechargePaymentManager private constructor(
    private val activity: AppCompatActivity,
    private var selectedAmount: String,
    private val viewModelScope: CoroutineScope,
    private var paymentStatusListener: PaymentStatusListener? = null
) {

    private val repository by lazy {
        ExpertListRepo()
    }

    fun startPayment(){
        paymentStatusListener?.onWarmUpStarted()
        viewModelScope.launch {
            repository.orderDetails
                .catch {
                    paymentStatusListener?.onWarmUpEnded("Something Went Wronggg")
                }
                .collectLatest {
                    startPaymentGateway()
                }
        }
    }

    fun startPaymentGateway(){
        showToast("Payement Done of $selectedAmount")
//        val checkout = Checkout()
//        checkout.setImage(R.mipmap.ic_launcher)
//        checkout.setKeyID(orderDetails.razorpayKeyId)
//        try {
//            val preFill = JSONObject()
//
//            if (User.getInstance().email.isNullOrEmpty().not()) {
//                preFill.put("email", User.getInstance().email)
//            } else {
//                preFill.put("email", Utils.getUserPrimaryEmail(activity.applicationContext))
//            }
//            //preFill.put("contact", "9999999999")
//
//            val options = JSONObject()
//            options.put("key", orderDetails.razorpayKeyId)
//            options.put("name", "Josh Skills")
//            try {
//                options.put(
//                    "description",
//                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName + "_app"
//                )
//            } catch (ex: Exception) {
//                ex.printStackTrace()
//            }
//            options.put("order_id", orderDetails.razorpayOrderId)
//            options.put("currency", orderDetails.currency)
//            options.put("amount", orderDetails.amount * 100)
//            options.put("prefill", preFill)
//            paymentStatusListener?.onWarmUpEnded()
//            checkout.open(activity, options)
//            razorpayOrderId = orderDetails.razorpayOrderId
//        } catch (e: Exception) {
//            e.printStackTrace()
//            paymentStatusListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
//        }
    }

    fun showErrorToast(){
        showToast("Something Went Wrong, Please Try Again")
    }

    fun onPaymentSuccess(status: String?){
        // TODO: Network Call for success
        // TODO: Show Dialog
        // TODO: Update Balance into db

    }

    fun onPaymentFailed(status: Int, message: String?){
        // TODO: Do Network Call
        showToast("Something Went Wrong")
    }


   /**
    Builder for Wallet Payment Manager.
    */

    data class Builder(
        var activity: AppCompatActivity? = null,
        var selectedAmount: String? = null,
        var coroutineScope: CoroutineScope? = null,
        var paymentStatusListener: PaymentStatusListener? = null
    ) {

        fun setActivity(activity: AppCompatActivity) = apply { this.activity = activity }

        fun setSelectedAmount(selectedAmount: String) = apply { this.selectedAmount = selectedAmount }

        fun setCoroutineScope(coroutineScope: CoroutineScope) = apply { this.coroutineScope = coroutineScope }

        fun setPaymentListener(paymentStatusListener: PaymentStatusListener) = apply { this.paymentStatusListener = paymentStatusListener }

        fun build(): WalletRechargePaymentManager{
            return WalletRechargePaymentManager(activity!!, selectedAmount!!, coroutineScope!!, paymentStatusListener)
        }

    }

}

interface PaymentStatusListener {
    fun onWarmUpStarted()
    fun onWarmUpEnded(error: String? = null)
}