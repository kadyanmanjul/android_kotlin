package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.databinding.FragmentWalletTransactionsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletTransactionsAdapter
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletTransactions(val viewModel: WalletTransactionViewModel) : Fragment() {

    private lateinit var binding:FragmentWalletTransactionsBinding

    private val adapter by lazy { WalletTransactionsAdapter() }

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