package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.CLICKED_CONTINUE_TO_CALL
import com.joshtalks.joshskills.databinding.FragmentCallCoutinueDialogBinding
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel

class CallContinueDialog : BaseDialogFragment() {

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    lateinit var  binding : FragmentCallCoutinueDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallCoutinueDialogBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnYes.setOnClickListener {
            callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_CONTINUE_TO_CALL)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CallContinueDialog().apply {
                arguments = Bundle().apply {

                }
            }
    }
}