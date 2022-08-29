package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.CLICKED_CONTINUE_TO_CALL
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentCallCoutinueDialogBinding
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.ExpertListViewModel

class CallContinueDialog : BaseDialogFragment() {

     var expertId: String = EMPTY
     var expertName: String = EMPTY
     var expertImage: String = EMPTY

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    lateinit var binding: FragmentCallCoutinueDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            expertId = it.getString(EXPERT_ID) ?: EMPTY
            expertName = it.getString(EXPERT_NAME) ?: EMPTY
            expertImage = it.getString(EXPERT_IMAGE) ?: EMPTY
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
        binding.textContinueCall.text = getString(R.string.continue_call_with_suman_sharma, expertName)
        binding.btnYes.setOnClickListener {
            callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_CONTINUE_TO_CALL)
            WalletRechargePaymentManager.selectedExpertForCall?.let { it1 -> expertListViewModel.getCallStatus(it1) }
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val EXPERT_ID = "mentor_id"
        const val EXPERT_NAME = "mentor_name"
        const val EXPERT_IMAGE = "mentor_image"

        @JvmStatic
        fun newInstance(mentorId: String, expertName: String, expertImage: String) =
            CallContinueDialog().apply {
                arguments = Bundle().apply {
                    putString(EXPERT_ID, mentorId)
                    putString(EXPERT_NAME, expertName)
                    putString(EXPERT_IMAGE, expertImage)
                }
            }

        fun open(supportFragmentManager: FragmentManager, mentorId: String, expertName: String, expertImage: String) {
            newInstance(mentorId, expertName, expertImage).show(supportFragmentManager, "CallContinueDialog")
        }
    }


}