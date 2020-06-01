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
import kotlinx.android.synthetic.main.fragment_payment_failed_dialog.*
import java.net.URLEncoder

class PaymentFailedDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentFailedDialogBinding
    private var courseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getString(COURSE_ID)
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
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        retry.setOnClickListener { dismiss() }
        chat_pay.setOnClickListener { openWhatsapp() }
    }

    private fun openWhatsapp() {
        val text = "Pay on Whatsapp"
        val phoneNumber = "91xxxxxxxxxx"
        val whatsappUrl = generateWhatsappUrl(text, phoneNumber)

        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(whatsappUrl)
        }
        startActivity(intent)

    }

    private fun generateWhatsappUrl(text: String?, phoneNumber: String?) =
        if (text != null) {
            "https://wa.me/${phoneNumber ?: ""}?text=${URLEncoder.encode(text, "utf-8")}"
        } else {
            "https://wa.me/${phoneNumber ?: ""}"
        }

    companion object {
        @JvmStatic
        fun newInstance(courseId: String) =
            PaymentFailedDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(COURSE_ID, courseId)
                }
            }
    }
}
