package com.joshtalks.joshskills.voip.audiocontroller

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Require Permissions :-
 * 1.Bluetooth
 * 2.Bluetooth Connect
 * 3.Modify Audio Settings
 *
 * Require Receivers @BluetoothReceiver @HeadsetReceiver with these filter :-
 * 1.ACTION_CONNECTION_STATE_CHANGED
 * 2.HEADSET_PLUG
 *
 * Require to Call Method @registerAudioControllerReceivers() to explicitly registering above receivers
 */

class AudioController(val coroutineScope : CoroutineScope) : AudioControllerInterface {

    private val applicationContext=Utils.context
    private val audioManager = Utils.context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private val audioRouteFlow = MutableSharedFlow<AudioRouteConstants>()
    companion object
    {
        private var isSpeakerOn : Boolean = false
        private var isHeadsetOn : Boolean = false
        private var isBluetoothOn : Boolean = false

    }
    private val headsetReceiver by lazy {
        HeadsetReceiver(coroutineScope)
    }
    private val bluetoothReceiver by lazy {
        BluetoothReceiver(coroutineScope)
    }
    init {
        coroutineScope.launch {
            try{
                headsetReceiver.observeHeadsetEvents().collect {
                    try{
                        when(it){
                            AudioRouteConstants.Default -> {
                                changeRouteStatus(isHeadsetOn = false)
                            }
                            AudioRouteConstants.HeadsetAudio -> {
                                changeRouteStatus(isHeadsetOn = true)
                            }
                        }

                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
        coroutineScope.launch {
            try{
                bluetoothReceiver.observeBluetoothEvents().collect {
                    try{
                        when(it){
                            AudioRouteConstants.BluetoothAudio -> {
                                changeRouteStatus(isBluetoothOn = true)
                            }
                            AudioRouteConstants.Default ->{
                                changeRouteStatus(isBluetoothOn = false)
                            }
                        }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }

        }
        setAudioRoutes()
    }

    private fun setAudioRoutes() {
        isHeadsetOn = audioManager?.isWiredHeadsetOn?:false
        isBluetoothOn = audioManager?.isBluetoothScoOn?:false
    }

    override fun registerAudioControllerReceivers() {
        val headsetFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        applicationContext?.registerReceiver(headsetReceiver, headsetFilter)
        applicationContext?.registerReceiver(bluetoothReceiver, bluetoothFilter)
    }

    override fun unregisterAudioControllerReceivers() {
        applicationContext?.unregisterReceiver(headsetReceiver)
        applicationContext?.unregisterReceiver(bluetoothReceiver)
    }

    override fun observeAudioRoute(): SharedFlow<AudioRouteConstants> {
        return audioRouteFlow
    }

    override fun getCurrentAudioRoute(): AudioRouteConstants {
        if (isSpeakerOn) {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.stopBluetoothSco()
            audioManager?.isBluetoothScoOn = false
            audioManager?.isSpeakerphoneOn = true
            return AudioRouteConstants.SpeakerAudio
        }
        if (isBluetoothOn) {
            CoroutineScope(Dispatchers.Main).launch {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.startBluetoothSco()
                audioManager?.isBluetoothScoOn = true
            }
            return AudioRouteConstants.BluetoothAudio
        }

        if (isHeadsetOn) {
            audioManager?.stopBluetoothSco()
            audioManager?.isBluetoothScoOn = false
            audioManager?.isSpeakerphoneOn = false
            return AudioRouteConstants.HeadsetAudio
        }


        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.stopBluetoothSco();
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = false

        return AudioRouteConstants.EarpieceAudio
    }

    override fun switchAudioToSpeaker() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.stopBluetoothSco()
        audioManager?.isBluetoothScoOn = false
        audioManager?.isSpeakerphoneOn = true
        isSpeakerOn=true
        checkAudioRoute()
    }

    override fun switchAudioToDefault() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = false
        isSpeakerOn=false
        checkAudioRoute()
    }

    override fun resetAudioRoute() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = false
    }

    internal fun checkAudioRoute() {
        emitAudioRoute(getCurrentAudioRoute())
    }

    private fun emitAudioRoute(audioRoute: AudioRouteConstants) {
        coroutineScope.launch {
            try{
                audioRouteFlow.emit(audioRoute)
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    fun changeRouteStatus(isBluetoothOn:Boolean? =null,isHeadsetOn:Boolean? = null){
        if (isHeadsetOn != null) {
            Companion.isHeadsetOn = isHeadsetOn
        }
        if (isBluetoothOn != null) {
            Companion.isBluetoothOn = isBluetoothOn
        }
        checkAudioRoute()
    }
}