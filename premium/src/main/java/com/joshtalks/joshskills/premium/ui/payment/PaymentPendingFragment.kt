package com.joshtalks.joshskills.premium.ui.payment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.premium.databinding.FragmentPaymentPendingBinding
import com.joshtalks.joshskills.premium.ui.inbox.InboxActivity
import com.joshtalks.joshskills.premium.ui.payment.viewModel.PaymentInProcessViewModel

class PaymentPendingFragment : Fragment() {

    private lateinit var binding: FragmentPaymentPendingBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[PaymentInProcessViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentPendingBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGoToInbox.setOnClickListener {
            startActivity(Intent(requireActivity(), InboxActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            requireActivity().finish()
        }
    }
}