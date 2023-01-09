package com.joshtalks.joshskills.expertcall.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.CLICKED_PROCEED
import com.joshtalks.joshskills.common.core.WALLET_SCREEN
import com.joshtalks.joshskills.common.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.expertcall.databinding.FragmentWalletBinding
import com.joshtalks.joshskills.expertcall.adapter.AmountAdapter
import com.joshtalks.joshskills.expertcall.model.Amount
import com.joshtalks.joshskills.expertcall.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.expertcall.utils.removeRupees
import com.joshtalks.joshskills.expertcall.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.expertcall.viewModel.WalletViewModel

class WalletFragment : Fragment() {

    private lateinit var binding: FragmentWalletBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
    }

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }


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
                    this@WalletFragment.viewModel.updateAddedAmount(amount.amount.toString())
                    amount.id = viewModel.commonTestId
                    callWithExpertViewModel.updateAmount(amount)
                }
        }
    }

    fun openCheckoutScreen() {
        viewModel.addedAmount.value?.let { addedAmount ->
            when{
                addedAmount == "" -> { showToast(getString(R.string.please_select_amount_to_add)) }
                addedAmount.removeRupees().toInt() > 20000 -> showToast(getString(R.string.maximum_amount_is_inr_20000))
                addedAmount.removeRupees().toInt() < 50 -> { showToast(getString(R.string.minimum_amount_required)) }
                else -> {
                    WalletRechargePaymentManager.isWalletOrUpgradePaymentType = "Wallet"
                    WalletRechargePaymentManager.selectedExpertForCall = null
                    callWithExpertViewModel.isPaymentInitiated = true
                    callWithExpertViewModel.updateAmount(Amount(viewModel.addedAmount.value!!.removeRupees().toInt(), viewModel.getAmountId()))
                    callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_PROCEED, previousPage = WALLET_SCREEN)
                    callWithExpertViewModel.proceedPayment()
                }
            }
        } ?: showToast(getString(R.string.please_select_amount_to_add))
    }

}