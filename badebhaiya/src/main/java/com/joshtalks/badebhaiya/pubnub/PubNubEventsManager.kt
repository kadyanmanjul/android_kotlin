package com.joshtalks.badebhaiya.pubnub

import android.util.Log
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME

/**
    This Object is Responsible to Send Events to PubNub which are triggered by UI directly.
*/

object PubNubEventsManager {

    fun sendHandRaisedEvent(isRaised: Boolean){
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("is_hand_raised", isRaised)
        customMessage.addProperty("short_name", PubNubManager.currentUser?.name ?: DEFAULT_NAME)
        customMessage.addProperty("action", "IS_HAND_RAISED")
        PubNubManager.sendCustomMessage(customMessage, PubNubManager.getLiveRoomProperties().moderatorId.toString())
    }

     fun sendMuteEvent(isMicOn: Boolean) {
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("is_mic_on", isMicOn)
        customMessage.addProperty("action", "MIC_STATUS_CHANGES")
        PubNubManager.sendCustomMessage(customMessage)
    }

    fun sendInviteUserEvent(uid: Int){
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("uid", uid)
        customMessage.addProperty("action", "INVITE_SPEAKER")
        PubNubManager.updateInviteSentToUserForSpeaker(uid)
        PubNubManager.sendCustomMessage(customMessage, uid.toString())
    }

    fun moveToSpeakerEvent(){
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("is_speaker", true)
        customMessage.addProperty("is_mic_on", false)
        customMessage.addProperty("short_name", PubNubManager.currentUser?.name ?: DEFAULT_NAME)
        customMessage.addProperty("action", "MOVE_TO_SPEAKER")
        PubNubManager.sendCustomMessage(customMessage)
    }

    fun moveToAudience(userUid: String, userName: String){
        val customMessage = JsonObject()
        customMessage.addProperty("id", userUid)
        customMessage.addProperty("is_speaker", false)
        customMessage.addProperty("short_name", userName)
        customMessage.addProperty("is_mic_on", false)
        customMessage.addProperty("action", "MOVE_TO_AUDIENCE")
//        PubNubManager.updateInviteSentToUserForSpeaker(userUid.toInt())
        PubNubManager.sendCustomMessage(customMessage)

    }

    fun inviteToSpeakerWithMicOff(userUid: String){
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("uid", userUid)
        customMessage.addProperty("is_mic_on", false)
        customMessage.addProperty("action", "INVITE_SPEAKER")
        PubNubManager.updateInviteSentToUserForSpeaker(userUid.toInt())
        PubNubManager.sendCustomMessage(customMessage, userUid)
    }

    fun userInvitedToSpeak(userId: Int?){
        PubNubManager.updateInviteSentToUser(userId)
        //todo check speaker list size
        val customMessage = JsonObject()
        customMessage.addProperty("id", PubNubManager.getLiveRoomProperties().agoraUid)
        customMessage.addProperty("uid", userId)
        customMessage.addProperty("action", "INVITE_SPEAKER")
        userId?.let {
            PubNubManager.updateInviteSentToUserForSpeaker(it)
            PubNubManager.sendCustomMessage(customMessage, userId.toString())
        }
    }

    fun removeHandRaise(){
        sendHandRaisedEvent(false)
    }

    fun sendModeratorStatus(status: Boolean, channelName: String?) {
        Log.i("MODERATORSTATUS", "sendModeratorStatus: $channelName")
        val customMessage = JsonObject()
        customMessage.addProperty("is_speaker_joined", status)
        PubNubManager.sendCustomMessage(customMessage, channelName+"waitingRoom")

    }

}