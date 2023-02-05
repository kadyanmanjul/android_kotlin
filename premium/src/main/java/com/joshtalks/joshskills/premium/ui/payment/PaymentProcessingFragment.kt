package com.joshtalks.joshskills.premium.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.premium.R

class PaymentProcessingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment_processing, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = PaymentProcessingFragment()
    }

}
