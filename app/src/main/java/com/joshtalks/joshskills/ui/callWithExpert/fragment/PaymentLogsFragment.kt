package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.databinding.FragmentPaymentLogsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletLogsAdapter
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletTransactionViewModel

class PaymentLogsFragment(val viewModel: WalletTransactionViewModel) : Fragment() {

    private lateinit var binding:FragmentPaymentLogsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPaymentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.paymentHistory.observe(viewLifecycleOwner){
            if (it.isNullOrEmpty().not()){
                binding.rvHistory.adapter = WalletLogsAdapter(it)
            }
        }
    }

}