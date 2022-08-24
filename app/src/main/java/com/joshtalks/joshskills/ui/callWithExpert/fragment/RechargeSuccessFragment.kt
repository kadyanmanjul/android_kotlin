package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment

class RechargeSuccessFragment : BaseDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recharge_success, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RechargeSuccessFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}