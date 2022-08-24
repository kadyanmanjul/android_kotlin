package com.joshtalks.joshskills.ui.callWithExpert

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.utils.PaymentStatusListener
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.razorpay.PaymentResultListener

class CallWithExpertActivity : AppCompatActivity(), PaymentResultListener, PaymentStatusListener {

    private lateinit var binding: ActivityCallWithExpertBinding
    private lateinit var balanceTv: TextView

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
                        .setPaymentListener(this)
                        .build()

                    walletPaymentManager.startPayment()
                }
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
        if (destination.id == R.id.walletFragment) {
            binding.toolbarContainer.ivEarn.gone()
        } else {
            binding.toolbarContainer.ivEarn.visible()
        }
    }

    companion object {
        fun open(activity: AppCompatActivity) {
            Intent(activity, CallWithExpertActivity::class.java).also {
                activity.startActivity(it)
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
    }

    override fun onPaymentSuccess(p0: String?) {
        walletPaymentManager.onPaymentSuccess(p0)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        walletPaymentManager.onPaymentFailed(p0, p1)
    }

    override fun onWarmUpStarted() {
        runOnUiThread {
            binding.progressBar.visible()
        }
    }

    override fun onWarmUpEnded(error: String?) {
        runOnUiThread {
            binding.progressBar.gone()
        }
        error?.let {
            showToast(it)
        }
    }
}