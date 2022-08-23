package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.databinding.FragmentWalletBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.AmountAdapter
import com.joshtalks.joshskills.ui.callWithExpert.constant.getAmountList
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletViewModel

class WalletFragment : Fragment() {

    private lateinit var binding: FragmentWalletBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
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

        with(binding){
            amountList.addItemDecoration(GridSpacingItemDecoration(2, 20, false))
            amountList.adapter = AmountAdapter(resources.getStringArray(R.array.amount_list).toList()){ amount ->
                viewModel?.updateAddedAmount(amount)
                openCheckoutScreen(amount)
            }
        }

    }

    private fun openCheckoutScreen(amount: String){
        // TODO: Open Checkout Fragment to enter coupon code.
    }

}