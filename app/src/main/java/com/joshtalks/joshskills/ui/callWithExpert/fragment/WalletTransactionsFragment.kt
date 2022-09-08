package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.databinding.FragmentWalletTransactionsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletTransactionsAdapter
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletTransactionViewModel

class WalletTransactions(val viewModel: WalletTransactionViewModel) : Fragment() {

    private lateinit var binding:FragmentWalletTransactionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentWalletTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.walletTransactions.observe(viewLifecycleOwner){
            if (it.isNullOrEmpty().not()){
                binding.rvHistory.adapter = WalletTransactionsAdapter(it)
            }
        }
    }
}