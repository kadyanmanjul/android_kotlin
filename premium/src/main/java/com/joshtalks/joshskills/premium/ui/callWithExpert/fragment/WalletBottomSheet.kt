package com.joshtalks.joshskills.premium.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.BOTTOM_SHEET
import com.joshtalks.joshskills.premium.core.CLICKED_PROCEED
import com.joshtalks.joshskills.premium.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.premium.databinding.BottomsheetWalletBinding
import com.joshtalks.joshskills.premium.ui.callWithExpert.adapter.AmountAdapter
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.WalletViewModel

class WalletBottomSheet(
    val amount: Int,
    val speakerName: String
) : BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetWalletBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
    }

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomsheetWalletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.minimumBalance.text = getString(
            R.string.minimum_balance_of_5_minutes,
            amount.toString(),
            speakerName
        )
        binding.amountListBottomSheet.addItemDecoration(
            GridSpacingItemDecoration(4, 20, false)
        )
        this.viewModel.availableAmount.observe(this) { amountList ->
            binding.amountListBottomSheet.adapter =
                AmountAdapter(amountList) { amount ->
                    amount.id = viewModel.commonTestId
                    callWithExpertViewModel.updateAmount(amount)
                    openCheckout()
                    dismiss()
                }
        }
    }

    fun openCheckout() {
        callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_PROCEED, previousPage = BOTTOM_SHEET)
        callWithExpertViewModel.proceedPayment()
    }

    companion object {
        const val TAG = "WalletBottomSheet"
    }
}