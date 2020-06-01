package com.joshtalks.joshskills.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R

class PaymentProcessingFragment : Fragment() {
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment_processing, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(courseId: String) =
            PaymentProcessingFragment().apply {
                arguments = Bundle().apply {
                    putString(COURSE_ID, courseId)
                }
            }
    }
}
