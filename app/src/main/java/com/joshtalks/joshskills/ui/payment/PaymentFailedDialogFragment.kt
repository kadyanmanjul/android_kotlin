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
import com.joshtalks.joshskills.databinding.FragmentPaymentFailedDialogBinding
import com.joshtalks.joshskills.ui.payment.order_summary.TRANSACTION_ID

const val WHATSAPP_URL_PAYMENT_FAILED = "http://english-new.joshtalks.org/whats_app/201"

class PaymentFailedDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentFailedDialogBinding
    private var transactionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            transactionId = it.getInt(TRANSACTION_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_payment_failed_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        binding.transationId.text = resources.getString(R.string.trx_id, transactionId.toString())
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        binding.retry.setOnClickListener { dismiss() }
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
            data = Uri.parse(WHATSAPP_URL_PAYMENT_FAILED)
        }
        startActivity(intent)
        activity?.finish()
    }

    companion object {
        @JvmStatic
        fun newInstance(transactionId: Int) =
            PaymentFailedDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(TRANSACTION_ID, transactionId)
                }
            }
    }
}
