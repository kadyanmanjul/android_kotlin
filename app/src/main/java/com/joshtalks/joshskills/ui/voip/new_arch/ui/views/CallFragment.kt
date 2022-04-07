package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.communication.constants.CLOSE_CALLING_FRAGMENT

private const val TAG = "CallFragment"
class CallFragment : BaseFragment() {

    lateinit var callBinding: FragmentCallBinding
    private val callbar = CallBar()

    val vm by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }
    val progressAnimator by lazy<ValueAnimator> {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            addUpdateListener {
                callBinding.incomingProgress.progress = ((animatedValue as Float) * 100).toInt()
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false)
        return callBinding.root
    }

    override fun initViewBinding() {
        callBinding.vm = vm
        progressAnimator.repeatCount= Animation.INFINITE
        progressAnimator.start()
        callBinding.executePendingBindings()
    }

    // TODO: Must be removed
    private fun startTimer() {
        val base = VoipPref.getStartTimeStamp()
        callBinding.callData = vm.getCallData()
        callBinding.callTime1.base = base
        callBinding.callTime1.start()
        progressAnimator.cancel()
        callBinding.incomingProgress.visibility=View.INVISIBLE
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when(it.what) {
                CLOSE_CALLING_FRAGMENT -> requireActivity().finish()
            }
        }

        callbar.getTimerLiveData().observe(viewLifecycleOwner) {
            Log.d(TAG, "initViewState: $it")
            startTimer()
        }
    }

    override fun setArguments() {}
}