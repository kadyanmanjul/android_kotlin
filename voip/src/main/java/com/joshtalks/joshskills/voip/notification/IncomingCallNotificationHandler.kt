package com.joshtalks.joshskills.voip.notification

import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.voip.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.voip.audiomanager.SoundManager
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL_CATEGORY
import com.joshtalks.joshskills.voip.mediator.*
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IncomingCallNotificationHandler : NotificationData.IncomingNotification{

    companion object{
        private val incomingCallMutex = Mutex(false)
        private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE, 20000) }
        private lateinit var voipNotification: VoipNotification
        private var isShowingIncomingCall = false
        private var calling : CallCategory = PeerToPeerCall()
        private var callCategory : Category = Category.PEER_TO_PEER
    }


    override fun inflateNotification(map: HashMap<String, String>) {
        when (map[INCOMING_CALL_CATEGORY]) {
            Category.PEER_TO_PEER.category -> {
                callCategory = Category.PEER_TO_PEER
                calling = PeerToPeerCall()
            }
            Category.FPP.category -> {
                callCategory = Category.FPP
                calling = FavoriteCall()
            }
            Category.GROUP.category -> {
                callCategory = Category.GROUP
                calling = GroupCall()
            }
        }

        if(!isShowingIncomingCall) {
            val remoteView = calling.notificationLayout(map) ?: return
            voipNotification = VoipNotification(remoteView, NotificationPriority.High)
            voipNotification.show()
            updateIncomingCallState(true)
            CallAnalytics.addAnalytics(
                event = EventName.INCOMING_CALL_SHOWN,
                agoraCallId = map[INTENT_DATA_INCOMING_CALL_ID],
                agoraMentorId = "-1"
            )
            soundManager.startRingtoneAndVibration()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(20000)
                    removeNotification()
                    CallAnalytics.addAnalytics(
                        event = EventName.INCOMING_CALL_IGNORE,
                        agoraCallId = map[INTENT_DATA_INCOMING_CALL_ID],
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
        stopAudio()
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
                    Companion.isShowingIncomingCall = isShowingIncomingCall
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }
}