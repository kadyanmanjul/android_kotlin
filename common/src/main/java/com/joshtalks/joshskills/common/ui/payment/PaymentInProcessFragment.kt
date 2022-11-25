package com.joshtalks.joshskills.common.ui.payment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.databinding.FragmentPaymentInProcessBinding
import com.joshtalks.joshskills.common.ui.payment.viewModel.PaymentInProcessViewModel

class PaymentInProcessFragment : Fragment() {

    private lateinit var binding: FragmentPaymentInProcessBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[PaymentInProcessViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPaymentInProcessBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.verifyPayment(arguments?.getString("ORDER_ID"))
    }
}