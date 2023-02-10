package com.joshtalks.joshskills.premium.calling.notification

import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.premium.calling.audiomanager.SOUND_TYPE_RINGTONE
import com.joshtalks.joshskills.premium.calling.constant.*
import com.joshtalks.joshskills.premium.calling.mediator.CallCategory
import com.joshtalks.joshskills.premium.calling.mediator.GroupCall
import com.joshtalks.joshskills.premium.calling.mediator.PeerToPeerCall
import com.joshtalks.joshskills.premium.calling.audiomanager.SoundManager
import com.joshtalks.joshskills.premium.calling.constant.*
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import com.joshtalks.joshskills.premium.calling.mediator.*
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.premium.calling.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IncomingCallNotificationHandler : NotificationData.IncomingNotification{

    private var ignoreNotificationScope = CoroutineScope(Dispatchers.IO)

    companion object{
        private val incomingCallMutex = Mutex(false)
        private val soundManager by lazy { SoundManager(SOUND_TYPE_RINGTONE, 20000) }
        private lateinit var voipNotification: VoipNotification
        private var isShowingIncomingCall = false
        private var calling : CallCategory = PeerToPeerCall()
        private var callCategory : Category = Category.PEER_TO_PEER
        private var currentIncomingCallNotificationId : Int? = null
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
            Category.EXPERT.category -> {
                callCategory = Category.EXPERT
                calling = ExpertCall()
                if (map[IS_PREMIUM_USER].equals("true"))
                    PrefManager.setExpertPremiumUser(true)
            }
        }
        if(!isShowingIncomingCall && PrefManager.getVoipState() == State.IDLE && PrefManager.getPstnState() == PSTN_STATE_IDLE) {
            val remoteView = calling.notificationLayout(map) ?: return
            voipNotification = VoipNotification(remoteView, NotificationPriority.High)
            voipNotification.show()
            currentIncomingCallNotificationId = voipNotification.getNotificationId()
            updateIncomingCallState(true)
            CallAnalytics.addAnalytics(
                event = EventName.INCOMING_CALL_SHOWN,
                agoraCallId = map[INTENT_DATA_INCOMING_CALL_ID],
                agoraMentorId = "-1"
            )
            soundManager.startRingtoneAndVibration()
            ignoreNotificationScope.launch {
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
        if (isShowingIncomingCall)
            voipNotification.removeNotification(currentIncomingCallNotificationId)
        stopAudio()
        updateIncomingCallState(false)
        ignoreNotificationScope.cancel()
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