package com.joshtalks.joshskills.ui.callWithExpert

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.fragment.RechargeSuccessFragment
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.payment.PaymentFailedDialogNew
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.ui.special_practice.utils.GATEWAY_INITIALISED
import com.joshtalks.joshskills.ui.special_practice.utils.PROCEED_PAYMENT_CLICK
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import kotlinx.coroutines.sync.Mutex
import java.math.BigDecimal

class CallWithExpertActivity : BaseActivity(), PaymentGatewayListener {
    private val backPressMutex = Mutex(false)

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
        setWhiteStatusBar()

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
            if (destination.id == R.id.paymentInProcessFragment)
                findViewById<View>(R.id.iv_back).gone()
            else
                findViewById<View>(R.id.iv_back).visible()
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

            if (viewModel.creditCount.value != -1)
                binding.toolbarContainer.toolbar.menu.findItem(R.id.upgrade_expert).isVisible = false

            binding.toolbarContainer.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.transaction_history -> {
                        navController.navigate(R.id.action_wallet_to_transactions)
                        viewModel.saveMicroPaymentImpression(OPEN_TRANSACTIONS, previousPage = MENU_TOOLBAR)
                    }
                    R.id.upgrade_expert -> {
                        navController.navigate(R.id.action_wallet_to_upgrade)
                        viewModel.saveMicroPaymentImpression(UPGRADE_PAGE_OPENED, previousPage = MENU_TOOLBAR)
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
    }

    private fun startPaymentForUpgrade(amount: Int, testId: Int) {
        WalletRechargePaymentManager.isWalletOrUpgradePaymentType = "Upgrade"
        WalletRechargePaymentManager.selectedExpertForCall = null
        viewModel.updateAmount(Amount(amount, testId))
        viewModel.proceedPayment()
        viewModel.isPaymentInitiated = true
        paymentManager.createOrderForExpert(testId = testId.toString(), amount = amount)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.extras?.getBoolean("open_upgrade_page") == true) {
            navController.navigate(R.id.expertCallUpgrade)
            viewModel.saveMicroPaymentImpression(UPGRADE_PAGE_OPENED, previousPage = CALL_DISCONNECTED)
        }
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.expertListFragment) {
            finish()
            return
        }
        if (viewModel.isPaymentInitiated) {
            backPressMutex.onMultipleBackPress {
                val backPressHandled = paymentManager.getJuspayBackPress()
                if (!backPressHandled) {
                    super.onBackPressed()
                }
            }
        } else
            super.onBackPressed()
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
        viewModel.saveBranchPaymentLog(
            paymentManager.getJustPayOrderId(),
            BigDecimal(paymentManager.getAmount()),
            testId = 0,
            courseName = "Spoken English Course",
        )

        MarketingAnalytics.coursePurchased(
            BigDecimal(paymentManager.getAmount()),
            true,
            testId = EMPTY,
            courseName = "Spoken English Course",
            juspayPaymentId = paymentManager.getJustPayOrderId()
        )
        viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        viewModel.isPaymentInitiated = false
        walletPaymentManager.onPaymentSuccess()
    }

    private fun showPaymentFailedDialog() {
        try {
            viewModel.isPaymentInitiated = false
            viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        navController.navigateUp()
        PaymentFailedDialogNew.newInstance(paymentManager, onCancelClick = {}).apply {
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

    private fun setWhiteStatusBar(){
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.pure_white)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            val windowInsetController =  WindowCompat.getInsetsController(window, window.decorView)
            windowInsetController.isAppearanceLightStatusBars = true
        }
    }
}