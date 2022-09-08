package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.databinding.FragmentPaymentLogsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletLogsAdapter
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PaymentLogsFragment(val viewModel: WalletTransactionViewModel) : Fragment() {

    private lateinit var binding:FragmentPaymentLogsBinding

    private val adapter by lazy { WalletLogsAdapter()}

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