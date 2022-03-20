package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel

class CallFragment : BaseFragment() {

    lateinit var callBinding: FragmentCallBinding

    val voiceCallViewModel by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false)
        return callBinding.root
    }

    override fun initViewBinding() {
        callBinding.vm = voiceCallViewModel
        callBinding.callData = voiceCallViewModel.getCallData()
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        TODO("Not yet implemented")
    }

    override fun setArguments() {
        TODO("Not yet implemented")
    }
}