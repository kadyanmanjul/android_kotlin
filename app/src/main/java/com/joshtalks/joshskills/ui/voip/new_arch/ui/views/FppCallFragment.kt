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
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.FROM_ACTIVITY
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.INTENT_DATA_FPP_IMAGE
import com.joshtalks.joshskills.base.constants.INTENT_DATA_FPP_NAME
import com.joshtalks.joshskills.databinding.FragmentFppCallBinding
import com.joshtalks.joshskills.ui.userprofile.adapters.setImage
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.setProfileImage
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.audiocontroller.AudioController
import com.joshtalks.joshskills.voip.audiocontroller.AudioRouteConstants
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED_EVENT
import com.joshtalks.joshskills.voip.constant.CANCEL_INCOMING_TIMER
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*

class FppCallFragment : BaseFragment() , SensorEventListener {
    private val TAG = "FppCallFragment"

    lateinit var callBinding: FragmentFppCallBinding
    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var powerManager: PowerManager? = null
    private  var lock: PowerManager.WakeLock? = null
    private var mPlayer: MediaPlayer? = null
    private var scope = CoroutineScope(Dispatchers.Main)
    private val audioController by lazy {
        AudioController(CoroutineScope((Dispatchers.IO)))
    }

    private var isFragmentRestarted = false

    val vm by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        callBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_fpp_call, container, false)
        return callBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(vm.source== FROM_ACTIVITY)
            startPlaying()
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
                Glide.with(this)
                    .load(image)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(callBinding.cImage)
            else
                Glide.with(this)
                    .load(R.drawable.ic_call_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(callBinding.cImage)
        }
    }

    override fun initViewState() {
        setUpProximitySensor()
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CANCEL_INCOMING_TIMER -> {
                    callBinding.groupUserdata.visibility = View.VISIBLE
                }
                CALL_INITIATED_EVENT -> {
                    callBinding.groupUserdata.visibility = View.VISIBLE
                    stopPlaying()
                    scope.cancel()
                }
            }
        }
    }

    override fun setArguments() {}


    private fun setUpProximitySensor() {
        try {
            sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            lock = powerManager?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "simplewakelock:wakelocktag"
            )
        }catch (ex : NullPointerException){
            ex.printStackTrace()
        }
    }

    private fun startPlaying() {
        scope.launch {
            try {
                mPlayer = MediaPlayer.create(requireContext(), R.raw.fpp_ringtone)
                mPlayer?.setVolume(0.5f, 0.5f)
                mPlayer?.isLooping = true
                mPlayer?.start()
                delay(50000)
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

    override fun onStart() {
        super.onStart()
        setCurrentCallState()
    }

    fun changeTopicImage(v:View){
        if (callBinding.topicViewpager.currentItem < callBinding.topicViewpager.adapter!!.itemCount)
            callBinding.topicViewpager.currentItem = callBinding.topicViewpager.currentItem + 1
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
            if (audioController.getCurrentAudioRoute() == AudioRouteConstants.EarpieceAudio) {
                turnScreenOff()
            }
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