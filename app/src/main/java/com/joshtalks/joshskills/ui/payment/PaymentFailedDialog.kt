package com.joshtalks.joshskills.ui.payment

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.R

class PaymentFailedDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_payment_failed, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(orderId: String) =
            PaymentFailedDialog().apply {
                arguments = Bundle().apply {

                }
            }
    }
}