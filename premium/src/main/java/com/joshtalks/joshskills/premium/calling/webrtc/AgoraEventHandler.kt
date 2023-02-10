package com.joshtalks.joshskills.premium.calling.webrtc

import android.util.Log
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.premium.calling.voipanalytics.EventName
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "AgoraEventHandler"
const val USER_QUIT_CHANNEL = 0
const val USER_DROP_OFFLINE = 1
private const val USER_ALREADY_LEFT_THE_CHANNEL = 18
private const val REMOTE_AUDIO_STATE_STARTING = 1
private const val AUDIO_PUBLISHED = 3
const val RECONNECTING_TIMEOUT_IN_MILLIS = 20 * 1000L

internal class AgoraEventHandler(val scope: CoroutineScope) : IRtcEngineEventHandler() {

    private val callingEvent by lazy {
        MutableSharedFlow<CallState>(replay = 0)
    }

    private val callingEventSpeakers by lazy {
        MutableSharedFlow<Array<out IRtcEngineEventHandler.AudioVolumeInfo>?>(replay = 0)
    }

    fun observeCallEvents() : SharedFlow<CallState> {
        return callingEvent
    }

    fun observeCallEventSpeaker() : SharedFlow<Array<out IRtcEngineEventHandler.AudioVolumeInfo>?>{
        return callingEventSpeakers
    }

    /**
     * 1. 1st ----> Join
     * +++++++++++++++++++++++
     * 2. Last ---> LeaveChannel ---- Clear all pending code execution
     */

    // Reports the volume information of users
    override fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    ) {
        emitSpeakers(speakers)
    }

    // Occurs when the local audio playback route changes
    override fun onAudioRouteChanged(routing: Int) {
        Log.d(TAG, "onAudioRouteChanged: $routing")
    }

    override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
        if(state == REMOTE_AUDIO_STATE_STARTING) {
            // LISTENING Event
                CallAnalytics.addAnalytics(
                    EventName.SPEAKING,
                    agoraMentorId = PrefManager.getLocalUserAgoraId().toString(),
                    agoraCallId = PrefManager.getAgraCallId().toString()
                )
            Log.d(TAG, "SPEAKER STARTED")
        }
    }


    override fun onAudioPublishStateChanged(
        channel: String?,
        oldState: Int,
        newState: Int,
        elapseSinceLastState: Int
    ) {
       if(newState == AUDIO_PUBLISHED) {
           // Mic Started
           Log.d(TAG, "MIC STARTED")
       }
    }


    // Reports an error during SDK runtime
    override fun onError(errorCode: Int) {
        Log.d(TAG, "onError: $errorCode")
        if(errorCode == USER_ALREADY_LEFT_THE_CHANNEL) {
            Log.d(TAG, "onError: USER ALREADY LEFT THE CHANNEL")
            emitEvent(CallState.UserAlreadyDisconnectedError)
            return
        }
        emitEvent(CallState.Error("In IRtcEngineEventHandler Class, onError Method Receive Error Code : $errorCode"))
    }

    // Occurs when the local user joins a specified channel (#joinChannel)
    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        Log.d(TAG, "onJoinChannelSuccess: $channel and UID -> $uid")
        emitEvent(CallState.CallInitiated)
    }

    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) joins the channel.
    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(TAG, "onUserJoined: UID -> $uid")
        emitEvent(CallState.CallConnected)
    }

    // Occurs when a user leaves the channel(#leaveChannel)
    override fun onLeaveChannel(stats: RtcStats) {
        Log.d(TAG, "onLeaveChannel: $stats")
        emitEvent(CallState.CallDisconnected)
    }


    // Occurs when a remote user (COMMUNICATION)/host (LIVE_BROADCASTING) leaves the channel.
    // 1. Leave the channel
    // 2. Drop offline
    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(TAG, "onUserOffline: $uid Reason -> $reason")
        scope.launch {
            try{
                if(reason == USER_DROP_OFFLINE && uid != PrefManager.getLocalUserAgoraId()) {
                    emitEvent(CallState.OnReconnecting)
                } else if(reason == USER_QUIT_CHANNEL) {
                    emitEvent(CallState.UserLeftChannel)
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    // Occurs when a user rejoins the channel after being disconnected due to network problems
    override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.d(TAG, "onRejoinChannelSuccess: Channel -> $channel and UID -> $uid")
        scope.launch {
            try{
                emitEvent(CallState.OnReconnected)
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    // Occurs when the SDK cannot reconnect to Agora server after its connection to the server is interrupted.
    override fun onConnectionLost() {
        Log.d(TAG, "onConnectionLost: ")
        // The SDK triggers this callback when it cannot connect to the server 10 seconds after calling joinChannel(), regardless of whether it is in the channel or not.
//        scope.launch {
//            emitEvent(CallState.CallDisconnect)
//        }
    }

    // Occurs when the network connection state changes like RECONNECTING
    override fun onConnectionStateChanged(state: Int, reason: Int) {
        Log.d(TAG, "onConnectionStateChanged: State - $state and Reason - $reason")
        scope.launch {
            try{
                if (Constants.CONNECTION_STATE_RECONNECTING == state &&
                    reason == Constants.CONNECTION_CHANGED_INTERRUPTED) {
                    emitEvent(CallState.OnReconnecting)
                }
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun emitEvent(event : CallState) {
        Log.d(TAG, " Emitting Event : $event")
        scope.launch {
            try{
                callingEvent.emit(event)
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun emitSpeakers(eventHandler: Array<out AudioVolumeInfo>?){
        scope.launch {
            try{
                callingEventSpeakers.emit(eventHandler)
            }
            catch (e : Exception){
                if(e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}