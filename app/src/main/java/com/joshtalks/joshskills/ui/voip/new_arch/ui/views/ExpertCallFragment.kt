package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.isValidContextForGlide
import com.joshtalks.joshskills.databinding.FragmentExpertCallBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

class ExpertCallFragment : BaseFragment() , SensorEventListener {
    private val TAG = "FppCallFragment"

    lateinit var callBinding: FragmentExpertCallBinding
    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var powerManager: PowerManager? = null
    private  var lock: PowerManager.WakeLock? = null
    private var mPlayer: MediaPlayer? = null
    private var scope = CoroutineScope(Dispatchers.Main)
    private val audioController by lazy {
        AudioController(CoroutineScope((Dispatchers.IO)))
    }

    var serice = null
    private var isFragmentRestarted = false

    val vm by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_expert_call, container, false)
        return callBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.isPermissionGranted.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if(vm.source== FROM_ACTIVITY && vm.isPermissionGranted.get()) {
                    startPlaying()
                }
            }
        })

        if(vm.source== FROM_ACTIVITY && vm.isPermissionGranted.get()) {
            startPlaying()
        }
        CallAnalytics.addAnalytics(
            event = EventName.CALL_SCREEN_SHOWN,
            agoraCallId = PrefManager.getAgraCallId().toString(),
            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
        )
    }

    override fun initViewBinding() {
        callBinding.vm = vm
        callBinding.executePendingBindings()
        val name = requireActivity().intent?.getStringExtra(INTENT_DATA_FPP_NAME)
        val image =requireActivity().intent?.getStringExtra(INTENT_DATA_FPP_IMAGE)

        if(vm.source== FROM_ACTIVITY) {
            Log.d(TAG, "setCallData: $name  $image")
            callBinding.callerName.text = name ?: "User Name"
            if (!image.isNullOrEmpty())
                if (isValidContextForGlide(requireContext())) {
                    Glide.with(this)
                        .load(image)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(callBinding.cImage)
                }
                else {
                    Glide.with(this)
                        .load(R.drawable.ic_call_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(callBinding.cImage)
                }
        }
    }

    override fun initViewState() {
        setUpProximitySensor()
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CANCEL_INCOMING_TIMER -> {

                }
                CALL_INITIATED_EVENT -> {

                    stopPlaying()
                    scope.cancel()
                }
            }
        }
    }

    override fun setArguments() {}

    private fun setUpProximitySensor() {
        Log.d(TAG, "onSensorChanged: 2")

        try {
            sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            lock = powerManager?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "simplewakelock:wakelock"
            )
        }catch (ex : NullPointerException){
            ex.printStackTrace()
            Log.d(TAG, "onSensorChanged: 3")

        }
    }

    private fun startPlaying() {
        scope.launch {
            try {
                mPlayer = MediaPlayer.create(requireContext(), R.raw.fpp_ringtone)
                mPlayer?.setVolume(0.5f, 0.5f)
                mPlayer?.isLooping = true
                mPlayer?.start()
                delay(20000)
                stopPlaying()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun stopPlaying() {
        try {
            mPlayer?.run {
                stop()
                release()
                mPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        proximity?.also { proximity ->
            sensorManager?.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStop() {
        super.onStop()
        sensorManager?.unregisterListener(this)
        if(lock?.isHeld == true) lock?.release()
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

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0?.values?.get(0)?.compareTo(0.0) == 0) {
            Log.d(TAG, "onSensorChanged: 1")
            turnScreenOff()
        } else {
            turnScreenOn()
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun turnScreenOff() {
        if (lock?.isHeld == false) lock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun turnScreenOn() {
        if (lock?.isHeld == true) lock?.release()
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        if(lock?.isHeld == true) lock?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
    }
}