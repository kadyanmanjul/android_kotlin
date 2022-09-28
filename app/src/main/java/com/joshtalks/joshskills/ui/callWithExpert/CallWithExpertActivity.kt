package com.joshtalks.joshskills.ui.callWithExpert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.OPEN_WALLET
import com.joshtalks.joshskills.core.SPEAKING_PAGE
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.fragment.RechargeSuccessFragment
import com.joshtalks.joshskills.ui.callWithExpert.utils.PaymentStatusListener
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager

class CallWithExpertActivity : AppCompatActivity(), PaymentStatusListener,
    PaymentGatewayListener {

    private lateinit var binding: ActivityCallWithExpertBinding
    private lateinit var balanceTv: TextView
    private val paymentManager: PaymentManager by lazy { PaymentManager(this, viewModel.viewModelScope, this) }

    private val viewModel by lazy {
        ViewModelProvider(this)[CallWithExpertViewModel::class.java]
    }

    private lateinit var navController: NavController

    private lateinit var walletPaymentManager: WalletRechargePaymentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_with_expert)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
//        PrefManager.initServicePref(applicationContext)
//        paymentManager = PaymentManager(this, viewModel.viewModelScope, this)
//        paymentManager.initializePaymentGateway()
        initToolbar()
        attachObservers()
        attachNavigationChangedListener()
        paymentManager.initializePaymentGateway()
    }

    private fun attachObservers() {
        viewModel.creditsCount.observe(this) {
            balanceTv.text = it
        }

        viewModel.proceedPayment.observe(this) { isProceed ->
            if (isProceed) {
                viewModel.addedAmount?.let {
                    walletPaymentManager = WalletRechargePaymentManager.Builder()
                        .setActivity(this)
                        .setSelectedAmount(it)
                        .setCoroutineScope(viewModel.viewModelScope)
                        .setPaymentGatewayListener(this)
                        .setPaymentListener(this)
                        .setNavController(navController)
                        .setPaymentManager(paymentManager)
                        .build()

                    walletPaymentManager.startPayment()
                }
            }
        }

        viewModel.isFirstAmount.observe(this){
            if (it.isFirstTime){
                RechargeSuccessFragment.open(supportFragmentManager, it.amount, isGifted = true,  type = "FirstTime")
            }
        }
    }

    private fun attachNavigationChangedListener() {
        navController =
            (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.toolbarContainer.textMessageTitle.text = destination.label
            manageBalanceIndicator(destination)
        }
    }

    private fun manageBalanceIndicator(destination: NavDestination) {
        if (destination.id == R.id.expertListFragment) {
            binding.toolbarContainer.ivEarn.visible()
        } else {
            binding.toolbarContainer.ivEarn.gone()
        }
        binding.toolbarContainer.toolbar.menu.clear()
        if(destination.id == R.id.walletFragment){
            binding.toolbarContainer.toolbar.inflateMenu(R.menu.wallet_menu)

            binding.toolbarContainer.toolbar.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.transaction_history -> {
                        navController.navigate(R.id.action_wallet_to_transactions)
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }
    }


    private fun initToolbar() {
        balanceTv = findViewById<TextView>(R.id.iv_earn)
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(findViewById<View>(R.id.iv_earn)){
            setOnClickListener {
                viewModel.saveMicroPaymentImpression(OPEN_WALLET, previousPage = SPEAKING_PAGE)
            }
        }
    }

    override fun onBackPressed() {
        val backPressHandled = paymentManager.getJuspayBackPress()
        if (!backPressHandled) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncCallDuration()
    }

    override fun onWarmUpEnded(error: String?) {
        runOnUiThread {
            binding.progressBar.gone()
        }
        error?.let {
            showToast(it)
        }
    }

    override fun onPaymentFinished(isPaymentSuccessful: Boolean) {
        if (isPaymentSuccessful){

        }
    }

    companion object {
        fun open(activity: AppCompatActivity) {
            Intent(activity, CallWithExpertActivity::class.java).also {
                activity.startActivity(it)
            }
        }
    }

    override fun onPaymentError(errorMsg: String) {
        Log.d("paymenterror", "onPaymentError:  $errorMsg")
        walletPaymentManager.onPaymentFailed(0, errorMsg)
    }

    override fun onPaymentSuccess() {
        viewModel.paymentSuccess(true)
        walletPaymentManager.onPaymentSuccess("onPaymentSuccess")
    }

    override fun onProcessStart() {
        runOnUiThread {
            binding.progressBar.visible()
        }
    }

    override fun onProcessStop() {
        runOnUiThread {
            binding.progressBar.gone()
        }
    }
}