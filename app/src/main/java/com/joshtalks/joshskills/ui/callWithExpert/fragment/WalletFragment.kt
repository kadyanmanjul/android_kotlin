package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CLICKED_PROCEED
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentWalletBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.AmountAdapter
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletViewModel

class WalletFragment : Fragment() {

    private lateinit var binding: FragmentWalletBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
    }

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    var amountToAdd = EMPTY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            amountList.addItemDecoration(GridSpacingItemDecoration(2, 20, false))

        }

        attachObservers()

    }

    private fun attachObservers() {
        viewModel.availableAmount.observe(viewLifecycleOwner) {
            binding.amountList.adapter =
                AmountAdapter(it) { amount ->
                    amountToAdd = amount.amountInRupees()
                    this@WalletFragment.viewModel.updateAddedAmount(amount.amountInRupees())
                    callWithExpertViewModel.updateAmount(amount)
                }
        }


        callWithExpertViewModel.paymentSuccessful.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigate(R.id.paymentProcessingFragment)
                callWithExpertViewModel.paymentSuccess(false)
            }
        }

    }

    fun openCheckoutScreen() {
        if (amountToAdd != EMPTY) {
            WalletRechargePaymentManager.selectedExpertForCall = null
            callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_PROCEED)
            callWithExpertViewModel.proceedPayment()
        }else{
            showToast("Please select amount to add")
        }
    }

}