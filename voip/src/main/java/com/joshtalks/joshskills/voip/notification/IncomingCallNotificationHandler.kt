package com.joshtalks.joshskills.voip.notification

import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.mediator.CallCategory
import com.joshtalks.joshskills.voip.mediator.PeerToPeerCall
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IncomingCallNotificationHandler : NotificationData.IncomingNotification{

    private val incomingCallMutex = Mutex(false)
    private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE, 20000) }
    private lateinit var voipNotification: VoipNotification
    private var isShowingIncomingCall = true
    val calling: CallCategory = PeerToPeerCall()

    override fun inflateNotification() {
        if(isShowingIncomingCall) {
            val incomingCall = IncomingCall(2827, Category.PEER_TO_PEER.ordinal, 221)
            val remoteView = calling.notificationLayout(incomingCall) ?: return
            voipNotification = VoipNotification(remoteView, NotificationPriority.High)
            voipNotification.show()
            updateIncomingCallState(true)
            CallAnalytics.addAnalytics(
                event = EventName.INCOMING_CALL_SHOWN,
                agoraCallId = IncomingCallData.callId.toString(),
                agoraMentorId = "-1"
            )
            soundManager.startRingtoneAndVibration()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(20000)
                    voipNotification.removeNotification()
                    stopAudio()
                    CallAnalytics.addAnalytics(
                        event = EventName.INCOMING_CALL_IGNORE,
                        agoraCallId = IncomingCallData.callId.toString(),
                        agoraMentorId = "-1"
                    )
                } catch (e: Exception) {
                    if (e is CancellationException)
                        throw e
                    e.printStackTrace()
                }
            }
        }
    }

    override fun removeNotification() {
        voipNotification.removeNotification()
        updateIncomingCallState(false)

    }

    override fun isNotificationVisible() :Boolean {
        return isShowingIncomingCall
    }

    private fun stopAudio() {
        try {
            soundManager.stopPlaying()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun updateIncomingCallState(isShowingIncomingCall: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                incomingCallMutex.withLock {
                    this@IncomingCallNotificationHandler.isShowingIncomingCall = isShowingIncomingCall
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

}