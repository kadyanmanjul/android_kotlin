package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.databinding.FragmentGroupCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupCallFragment : BaseFragment() {

    private val TAG = "GroupCallFragment"

    lateinit var callBinding: FragmentGroupCallBinding
    private var isAnimationCanceled = false

    private var isFragmentRestarted = false

    val vm by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    private val progressAnimator by lazy<ValueAnimator> {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            addUpdateListener {
                callBinding.incomingProgress.progress = ((animatedValue as Float) * 100).toInt()
            }
        }
    }

    private val textAnimator by lazy<ValueAnimator> {
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
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_call, container, false)
        return callBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CallAnalytics.addAnalytics(
            event = EventName.CALL_SCREEN_SHOWN,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
    }

    override fun initViewBinding() {
        callBinding.vm = vm
        callBinding.callFragment = this
        if (vm.source == FROM_INCOMING_CALL && PrefManager.getVoipState() != State.CONNECTED) {
            startIncomingTimer()
        }
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CANCEL_INCOMING_TIMER -> {
                    stopAnimation()
                    callBinding.incomingTimerContainer.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun startIncomingTimer() {
        stopAnimation()
        isAnimationCanceled = false
        var counter = 35
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                Log.d(TAG, "onAnimationEnd: $counter $isAnimationCanceled")
                if (counter != 0 && !isAnimationCanceled) {
                    counter -= 1
                    Log.d(TAG, "onAnimationEnd Inside: $counter $isAnimationCanceled")
                    callBinding.incomingTimerTv.text = "$counter"
                    textAnimator.start()
                    progressAnimator.start()
                }

                if (counter <= 0) {
                    Log.d(TAG, "onAnimationEnd: Disconnecting")
                    vm.disconnect()
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                if (textAnimator.isStarted && textAnimator.isRunning)
                    textAnimator.cancel()
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
        progressAnimator.start()
    }

    @Synchronized
    private fun stopAnimation() {
        isAnimationCanceled = true
        progressAnimator.cancel()
    }

    override fun setArguments() {}

    override fun onResume() {
        super.onResume()
        if (callBinding.incomingTimerContainer.visibility == View.VISIBLE) {
            CoroutineScope(Dispatchers.Main).launch {
                progressAnimator.resume()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setCurrentCallState()
    }


    private fun setCurrentCallState() {
        if (isFragmentRestarted) {
            if (vm.source == FROM_INCOMING_CALL && (PrefManager.getVoipState() == State.SEARCHING || PrefManager.getVoipState() == State.JOINING))
                return
            else if ((PrefManager.getVoipState() == State.JOINED || PrefManager.getVoipState() == State.CONNECTED).not())
                requireActivity().finish()
        } else
            isFragmentRestarted = true
    }
}