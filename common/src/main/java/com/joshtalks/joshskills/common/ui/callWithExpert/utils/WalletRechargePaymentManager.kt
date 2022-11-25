package com.joshtalks.joshskills.common.ui.callWithExpert.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.ui.callWithExpert.fragment.RechargeSuccessFragment
import com.joshtalks.joshskills.common.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.common.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.common.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.common.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.common.ui.paymentManager.PaymentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
This class is responsible to process payment for wallet recharge payment.
 */

class WalletRechargePaymentManager private constructor(
    private val activity: AppCompatActivity,
    private var selectedAmount: Amount,
    private val viewModelScope: CoroutineScope,
    private var paymentGatewayListener: PaymentGatewayListener? = null,
    private var navController: NavController? = null,
    private var paymentManager: PaymentManager? = null
) {

    private val expertListRepo by lazy {
        ExpertListRepo()
    }

    fun startPayment(testId: String, amount: Int) {
        viewModelScope.launch {
            try {
                paymentManager?.createOrderForExpert(testId = testId, amount = amount)
            } catch (e: Exception) {
                e.printStackTrace()
                paymentGatewayListener?.onWarmUpEnded(activity.getString(R.string.something_went_wrong))
            }
        }
    }

    fun onPaymentSuccess() {
        viewModelScope.launch {
            delay(5000)
            onPaymentFinished()
        }
        updateWalletBalance()
    }

    fun updateWalletBalance() {
        viewModelScope.launch {
            expertListRepo.updateWalletBalance()
        }
    }

    fun onPaymentFinished() {
        navController?.let {
            navController?.navigate(R.id.expertListFragment)
            RechargeSuccessFragment.open(
                activity.supportFragmentManager,
                amount = selectedAmount.amount,
                type = "Wallet"
            )
        }
    }

    /**
    Builder for Wallet Payment Manager.
     */
    data class Builder(
        var activity: AppCompatActivity? = null,
        var selectedAmount: Amount? = null,
        var coroutineScope: CoroutineScope? = null,
        var paymentGatewayListener: PaymentGatewayListener? = null,
        var navController: NavController? = null,
        var paymentManager: PaymentManager? = null,
    ) {

        fun setActivity(activity: AppCompatActivity) = apply { this.activity = activity }

        fun setSelectedAmount(selectedAmount: Amount) =
            apply { this.selectedAmount = selectedAmount }

        fun setCoroutineScope(coroutineScope: CoroutineScope) =
            apply { this.coroutineScope = coroutineScope }

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
                paymentGatewayListener,
                navController,
                paymentManager
            )
        }
    }

    companion object {
        const val TAG = "WalletPaymentManager"
        var selectedExpertForCall: ExpertListModel? = null
        var isWalletOrUpgradePaymentType : String = EMPTY
    }

}

//interface PaymentStatusListener {
//    fun onPaymentFinished(isPaymentSuccessful: Boolean)
//}