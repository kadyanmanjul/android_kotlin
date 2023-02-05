package com.joshtalks.joshskills.premium.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.premium.databinding.FragmentWalletTransactionsBinding
import com.joshtalks.joshskills.premium.ui.callWithExpert.adapter.WalletTransactionsAdapter
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.WalletTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletTransactions: Fragment() {

    private lateinit var binding:FragmentWalletTransactionsBinding

    private val adapter by lazy { WalletTransactionsAdapter() }
    private val viewModel by lazy { ViewModelProvider(this)[WalletTransactionViewModel::class.java]}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentWalletTransactionsBinding.inflate(inflater, container, false)
        binding.rvHistory.adapter = adapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch{
            viewModel.walletTransactionList.collectLatest {
                adapter.submitData(it)
            }
        }
    }
}