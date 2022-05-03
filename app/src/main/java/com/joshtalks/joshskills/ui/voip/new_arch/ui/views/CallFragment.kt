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
import com.joshtalks.joshskills.databinding.FragmentCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants
import com.joshtalks.joshskills.voip.communication.constants.CLOSE_CALLING_FRAGMENT
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED_EVENT
import com.joshtalks.joshskills.voip.constant.CONNECTED
import com.joshtalks.joshskills.voip.constant.JOINED
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "CallFragment"

class CallFragment : BaseFragment() , SensorEventListener {

    lateinit var callBinding: FragmentCallBinding
    private val callBar = CallBar()
    private var isAnimationCanceled = false
    private lateinit var sensorManager: SensorManager
    private lateinit var proximity: Sensor
    private lateinit var powerManager: PowerManager
    private lateinit var lock: PowerManager.WakeLock
    private val audioController by lazy {
        AudioController(CoroutineScope((Dispatchers.IO)))
    }

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
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false)
        return callBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        CallAnalytics.addAnalytics(
//            event = EventName.CALL_SCREEN_SHOWN,
//            agoraMentorId =  VoipPref.getCurrentUserAgoraId().toString(),
//            agoraCallId = VoipPref.getCurrentCallId().toString()
//        )
    }

    override fun initViewBinding() {
        callBinding.vm = vm
        if(vm.source == FROM_INCOMING_CALL && PrefManager.getVoipState() != CONNECTED) {
            startIncomingTimer()
        }
        callBinding.executePendingBindings()
    }

    override fun initViewState() {
        setUpProximitySensor()
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CLOSE_CALLING_FRAGMENT -> requireActivity().finish()
                CALL_CONNECTED_EVENT -> { isAnimationCanceled= true }
            }
        }
    }

    private fun setUpProximitySensor() {
        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        lock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,"simplewakelock:wakelocktag")
    }

    private fun startIncomingTimer() {
        stopAnimation()
        isAnimationCanceled = false
        var counter = 35
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                if (counter != 0 && !isAnimationCanceled) {
                    counter -= 1
                    callBinding.incomingTimerTv.text = "$counter"
                    textAnimator.start()
                    progressAnimator.start()
                }

                if (counter <= 0) {
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
        run{
            progressAnimator.cancel()
        }
    }

    override fun setArguments() {}

    override fun onResume() {
        super.onResume()
        proximity.also { proximity ->
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
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
            if((PrefManager.getVoipState() == JOINED || PrefManager.getVoipState() == CONNECTED).not())
                requireActivity().finish()
        } else
            isFragmentRestarted = true
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0?.values?.get(0)?.compareTo(0.0) == 0) {
            if (audioController.getCurrentAudioRoute() == AudioRouteConstants.EarpieceAudio) {
                turnScreenOff()
            }
        } else {
            turnScreenOn()
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun turnScreenOff() {
        if (!lock.isHeld) lock.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun turnScreenOn() {
        if (lock.isHeld) lock.release()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if(lock.isHeld) lock.release()
    }
}