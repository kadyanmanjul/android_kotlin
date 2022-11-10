package com.joshtalks.joshskills.ui.callWithExpert

import android.content.Intent
import android.os.Bundle
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
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.EXPERT_UPGRADE_CLICK
import com.joshtalks.joshskills.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.constants.PAYMENT_PENDING
import com.joshtalks.joshskills.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.core.OPEN_WALLET
import com.joshtalks.joshskills.core.SPEAKING_PAGE
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.fragment.RechargeSuccessFragment
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.payment.PaymentFailedDialogNew
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.ui.special_practice.utils.GATEWAY_INITIALISED
import com.joshtalks.joshskills.ui.special_practice.utils.PROCEED_PAYMENT_CLICK

class CallWithExpertActivity : BaseActivity(), PaymentGatewayListener {

    private lateinit var binding: ActivityCallWithExpertBinding
    private lateinit var balanceTv: TextView
    private val paymentManager: PaymentManager by lazy {
        PaymentManager(
            this,
            viewModel.viewModelScope,
            this
        )
    }

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

        initToolbar()
        attachObservers()
        attachNavigationChangedListener()
        paymentManager.initializePaymentGateway()
    }

    override fun initViewBinding() {
//        TODO("Not yet implemented")
    }

    override fun onCreated() {
//        TODO("Not yet implemented")
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                PAYMENT_SUCCESS -> onPaymentSuccess()
                PAYMENT_FAILED -> showPaymentFailedDialog()
                PAYMENT_PENDING -> {
                    navController.navigateUp()
                    navController.navigate(R.id.paymentPendingFragment)
                }
                EXPERT_UPGRADE_CLICK -> startPaymentForUpgrade(
                    amount = it.arg1,
                    testId = it.arg2
                )
            }
        }
    }

    private fun attachObservers() {
        viewModel.walletAmount.observe(this) {
            balanceTv.text = it.toString()
        }

        viewModel.proceedPayment.observe(this) { isProceed ->
            if (isProceed) {
                viewModel.addedAmount?.let {
                    walletPaymentManager = WalletRechargePaymentManager.Builder()
                        .setActivity(this)
                        .setSelectedAmount(it)
                        .setCoroutineScope(viewModel.viewModelScope)
                        .setPaymentGatewayListener(this)
                        .setNavController(navController)
                        .setPaymentManager(paymentManager)
                        .build()

                    walletPaymentManager.startPayment(it.id.toString(), it.amount)
                }
            }
        }

        viewModel.isFirstAmount.observe(this) {
            if (it.isFirstTime) {
                RechargeSuccessFragment.open(
                    supportFragmentManager,
                    it.amount,
                    isGifted = true,
                    type = "FirstTime"
                )
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
        if (destination.id == R.id.walletFragment) {
            binding.toolbarContainer.toolbar.inflateMenu(R.menu.wallet_menu)

            binding.toolbarContainer.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.transaction_history -> {
                        navController.navigate(R.id.action_wallet_to_transactions)
                    }
                    R.id.upgrade_expert -> {
                        navController.navigate(R.id.action_wallet_to_upgrade)
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }
    }

    private fun initToolbar() {
        balanceTv = findViewById(R.id.iv_earn)
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(findViewById<View>(R.id.iv_earn)) {
            setOnClickListener {
                viewModel.saveMicroPaymentImpression(OPEN_WALLET, previousPage = SPEAKING_PAGE)
            }
        }
    }

    private fun startPaymentForUpgrade(amount: Int, testId: Int) {
        paymentManager.createOrderForExpert(testId = testId.toString(), amount = amount)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.extras?.getBoolean("open_upgrade_page") == true)
            navController.navigate(R.id.expertCallUpgrade)
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
        if (isPaymentSuccessful) {

        }
    }

    override fun onPaymentProcessing(orderId: String, status:String) {
        if (status == "pending_vbv"){
            navController.navigate(R.id.paymentPendingFragment)
            return
        }
        val bundle = Bundle()
        bundle.putString("ORDER_ID", orderId)
        navController.navigate(R.id.paymentInProcessFragment, bundle)
    }

    companion object {
        fun open(activity: AppCompatActivity) {
            Intent(activity, CallWithExpertActivity::class.java).also {
                activity.startActivity(it)
            }
        }
    }

    private fun onPaymentSuccess() {
        viewModel.paymentSuccess(true)
        walletPaymentManager.onPaymentSuccess()
    }

    private fun showPaymentFailedDialog() {
        navController.navigateUp()
        PaymentFailedDialogNew.newInstance(paymentManager, onCancelClick = {
//            navController.navigateUp()
        }).apply {
            show(supportFragmentManager, "PAYMENT_FAILED")
        }
    }

    override fun onProcessStart() {
        viewModel.saveImpressionForPayment(PROCEED_PAYMENT_CLICK, "CALL_WITH_EXPERT")
        runOnUiThread {
            binding.progressBar.visible()
        }
    }

    override fun onProcessStop() {
        viewModel.saveImpressionForPayment(GATEWAY_INITIALISED, "CALL_WITH_EXPERT")
        runOnUiThread {
            binding.progressBar.gone()
        }
    }
}