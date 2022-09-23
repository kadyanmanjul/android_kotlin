package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import androidx.core.view.drawToBitmap
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.databinding.FragmentCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception


class   CallFragment : BaseFragment() {
    private val TAG = "CallFragment"

    lateinit var callBinding: FragmentCallBinding
    private var isAnimationCanceled = false
    private var audioRequest :AudioFocusRequest? = null
    private var isFragmentRestarted = false

    private val audioManager by lazy {
        requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val audioController by lazy {
        AudioController(CoroutineScope((Dispatchers.IO)))
    }

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
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false)
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
        if(vm.source == FROM_INCOMING_CALL && PrefManager.getVoipState() != State.CONNECTED) {
            startIncomingTimer()
        }
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        gainAudioFocus()

        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CANCEL_INCOMING_TIMER -> {
                    stopAnimation()
                    callBinding.incomingTimerContainer.visibility = View.INVISIBLE
                }

            }
        }
    }

    private fun gainAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           audioRequest =  AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    }
                )
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener{}.build()
            }
            audioRequest?.acceptsDelayedFocusGain()
            val result = audioRequest?.let { audioManager.requestAudioFocus(it) }
            Log.d(TAG, "gainAudioFocus 1: request result $result")
        } else {
            val result = audioManager.requestAudioFocus(
                { },
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
            )
            Log.d(TAG, "gainAudioFocus 2: request result $result")

        }
    }

    private fun startIncomingTimer() {
        stopAnimation()
        isAnimationCanceled = false
        var counter = 35
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
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
        isAnimationCanceled = true
        progressAnimator.cancel()
    }

    override fun setArguments() {}

    override fun onResume() {
        super.onResume()
        if (callBinding.incomingTimerContainer.visibility == View.VISIBLE) {
            CoroutineScope(Dispatchers.Main).launch{
                progressAnimator.resume()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setCurrentCallState()
    }

    private fun setCurrentCallState() {
        if(isFragmentRestarted) {
            if(vm.source == FROM_INCOMING_CALL && (PrefManager.getVoipState() == State.SEARCHING || PrefManager.getVoipState() == State.JOINING))
                return
            else if((PrefManager.getVoipState() == State.JOINED || PrefManager.getVoipState() == State.CONNECTED).not())
                requireActivity().finish()
        } else
            isFragmentRestarted = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }else {
            audioManager.abandonAudioFocus(null)
        }
    }
}