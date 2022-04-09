package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentCallBinding
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.analytics.CurrentCallDetails
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.util.DateUtils
import com.joshtalks.joshskills.voip.communication.constants.CLOSE_CALLING_FRAGMENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "CallFragment"

class CallFragment : BaseFragment() {

    lateinit var callBinding: FragmentCallBinding
    private val callbar = CallBar()
    private var isAnimationCancled = false


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
    val textAnimator by lazy<ValueAnimator> {
        ValueAnimator.ofFloat(0.8f, 1.2f, 1f).apply {
            duration = 300
            interpolator = BounceInterpolator()
            addUpdateListener {
                callBinding.incomingTimerTv.scaleX = it.animatedValue as Float
                callBinding.incomingTimerTv.scaleY = it.animatedValue as Float
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
        startIncomingTimer()
        callBinding.executePendingBindings()
    }

    // TODO: Must be removed
    private fun startTimer() {
        val base = VoipPref.getStartTimeStamp()
        callBinding.callData = vm.getCallData()
        callBinding.callTime1.base = base
        callBinding.callTime1.start()
        progressAnimator.cancel()
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CLOSE_CALLING_FRAGMENT -> requireActivity().finish()
                CALL_CONNECTED_EVENT -> {
                    isAnimationCancled= true
                }
            }
        }

        callbar.getTimerLiveData().observe(viewLifecycleOwner) {
            Log.d(TAG, "initViewState: $it")
            if (it > 0) {
                startTimer()
            }
        }
    }

    private fun startIncomingTimer() {
        stopAnimation()
        isAnimationCancled = false
//        setIncomingText()
        var counter = 35
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                if (counter != 0 && !isAnimationCancled) {
                    counter -= 1
                    callBinding.incomingTimerTv.text = "$counter"
                    textAnimator.start()
                    progressAnimator.start()
                }

                if (counter <= 0) {
//                     TODO:TO FINISH ACTIVITY
                }
            }
            override fun onAnimationCancel(animation: Animator?) {
                if (textAnimator.isStarted && textAnimator.isRunning)
                    textAnimator.cancel()
            }

            override fun onAnimationRepeat(animation: Animator?) {}
        })
        progressAnimator.start()
    }

    @Synchronized
    private fun stopAnimation() {
        Log.d(TAG, "stopAnimation: ")
        isAnimationCancled = true
        run{
            progressAnimator.cancel()
        }
    }

    override fun setArguments() {}

    override fun onPause() {
        if (callBinding.incomingTimerContainer.visibility == View.VISIBLE) {
            isAnimationCancled = true
            CoroutineScope(Dispatchers.Main).launch{
                progressAnimator.cancel()
            }
        }
        super.onPause()
    }
}