package com.joshtalks.joshskills.expertcall.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.expertcall.databinding.FragmentPaymentLogsBinding
import com.joshtalks.joshskills.expertcall.adapter.WalletLogsAdapter
import com.joshtalks.joshskills.expertcall.viewModel.WalletTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PaymentLogsFragment() : Fragment() {

    private lateinit var binding:FragmentPaymentLogsBinding

    private val adapter by lazy { WalletLogsAdapter()}

    private val viewModel by lazy { ViewModelProvider(this)[WalletTransactionViewModel::class.java]}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentLogsBinding.inflate(inflater, container, false)
        binding.rvHistory.adapter = adapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewModel.walletPaymentLogsList.collectLatest {
                adapter.submitData(it)
            }
        }
    }
}