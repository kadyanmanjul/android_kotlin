package com.joshtalks.joshskills.premium.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.premium.base.BaseDialogFragment
import com.joshtalks.joshskills.premium.core.CLICKED_CONTINUE_TO_CALL
import com.joshtalks.joshskills.premium.databinding.FragmentCallCoutinueDialogBinding
import com.joshtalks.joshskills.premium.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.ExpertListViewModel

class CallContinueDialog : BaseDialogFragment(true) {

    private val callWithExpertViewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    var selectedUser: ExpertListModel? = null

    lateinit var binding: FragmentCallCoutinueDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedUser = WalletRechargePaymentManager.selectedExpertForCall
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallCoutinueDialogBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = selectedUser
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textContinueCall.text = "Continue call with " + selectedUser?.expertName

        binding.btnYes.setOnClickListener {
            callWithExpertViewModel.saveMicroPaymentImpression(CLICKED_CONTINUE_TO_CALL)
            selectedUser?.let { it1 -> expertListViewModel.getCallStatus(it1) }
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CallContinueDialog().apply {
                arguments = Bundle().apply {

                }
            }

        fun open(supportFragmentManager: FragmentManager, ) {
            newInstance().show(supportFragmentManager, "CallContinueDialog")
        }
    }


}