package com.joshtalks.joshskills.ui.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentChatNPayBinding

const val WHATSAPP_URL_BANGLADESH = "http://english-new.joshtalks.org/whats_app/202"

class ChatNPayDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChatNPayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_chat_n_pay,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        binding.chatPay.setOnClickListener { openWhatsapp() }
        binding.close.setOnClickListener { dismissAndCloseActivity() }
    }

    private fun dismissAndCloseActivity() {
        dismiss()
        activity?.finish()
    }

    private fun openWhatsapp() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(WHATSAPP_URL_BANGLADESH)
        }
        startActivity(intent)
        activity?.finish()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PaymentFailedDialogFragment()
    }
}
