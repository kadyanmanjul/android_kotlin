package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.joshtalks.joshskills.BR
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.RecordingButtonState
import com.joshtalks.joshskills.voip.data.UIState
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.getMentorName
import com.joshtalks.joshskills.voip.getMentorProfile

interface CallData{
    fun getProfileImage(): String?
    fun getCallerName():String
    fun getTopicName():String
    fun getCallType(): Int
    fun getCallTypeHeader():String
    fun getStartTime():Long
}
sealed class CallType{
    object FavoritePracticePartner:CallType()
    object NormalPracticePartner:CallType()
    object GroupCall:CallType()
}

class CallUIState : BaseObservable() {

    companion object {
        private var isGameStarted = false
    }

    @get:Bindable
    var profileImage: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.profileImage)
        }

    @get:Bindable
    var currentState: String = "Connecting..."
        set(value) {
            field = value
            updateState(field, value, BR.currentState)
        }

    @get:Bindable
    var recordingCurrentState: String = "Waiting for your partner to accept"
        set(value) {
            field = value
            updateState(field, value, BR.recordingCurrentState)
        }

    @get:Bindable
    var name: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.name)
        }

    @get:Bindable
    var topic: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.topic)
        }

    @get:Bindable
    var type: Int = 0
        set(value) {
            field = value
            updateState(field, value, BR.type)
        }

    @get:Bindable
    var title: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.title)
        }

    @get:Bindable
    var visibleCrdView: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.visibleCrdView)
        }

    @get:Bindable
    var startTime: Long = 0L
        set(value) {
            field = value
            updateState(field, value, BR.startTime)
        }

    @get:Bindable
    var recordTime: Long = 0L
        set(value) {
            field = value
            updateState(field, value, BR.recordTime)
        }

    @get:Bindable
    var recordBtnImg: Int = R.drawable.call_fragment_record
        set(value) {
            field = value
            updateState(field, value, BR.recordBtnImg)
        }

    @get:Bindable
    var recordCrdViewTxt: String = "Waiting for your partner to accept"
        set(value) {
            field = value
            updateState(field, value, BR.recordCrdViewTxt)
        }

    @get:Bindable
    var recordBtnTxt: String = "Record"
        set(value) {
            field = value
            updateState(field, value, BR.recordBtnTxt)
        }

    @get:Bindable
    var isMute: Boolean = false
        set(value) {
            Log.d("naman", "$field:$value ")
            field = value
            updateState(field, value, BR.mute)
        }

    @get:Bindable
    var test: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.test)
        }

    @get:Bindable
    var isRecordingEnabled: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.recordingEnabled)
        }

    @get:Bindable
    var recordingButtonState: RecordingButtonState = RecordingButtonState.SENTREQUEST
        set(value) {
            field = value
            updateState(field, value, BR.recordingButtonState)
        }

    @get:Bindable
    var recordingStartTime: Long = 0L
        set(value) {
            field = value
            updateState(field, value, BR.recordingStartTime)
        }

    @get:Bindable
    var isSpeakerOn: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.speakerOn)
        }

    @get:Bindable
    var isOnHold: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.onHold)
        }

    @get:Bindable
    var isRemoteUserMuted: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.remoteUserMuted)
        }

    @get:Bindable
    var topicImage: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.topicImage)
        }

    @get:Bindable
    var gameWord: String = ""
        set(value) {
            if (isGameStarted) {
                field = value
                updateState(field, value, BR.gameWord)
            }
        }

    @get:Bindable
    var wordColor: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.wordColor)
        }

    @get:Bindable
    var isNextWordClicked: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.nextWordClicked)
        }

    @get:Bindable
    var isStartGameClicked: Boolean = false
        set(value) {
            field = value
            field = value
            updateState(field, value, BR.startGameClicked)
        }

    @get:Bindable
    var occupation: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.occupation)
        }

    @get:Bindable
    var aspiration: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.aspiration)
        }

    @get:Bindable
    var localUserName: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.localUserName)
        }

    @get:Bindable
    var localUserProfile: String = ""
        set(value) {
            field = value
            updateState(field, value, BR.localUserProfile)
        }

    @get:Bindable
    var isCalleeSpeaking: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.calleeSpeaking)
        }

    @get:Bindable
    var isCallerSpeaking: Boolean = false
        set(value) {
            field = value
            updateState(field, value, BR.callerSpeaking)
        }

    @get:Bindable
    var recordButtonPressedTwoTimes: Int = 0
        set(value) {
            field = value
            updateState(field, value, BR.recordButtonPressedTwoTimes)
        }

    @get:Bindable
    var p2pCallBackgroundColor: Int = R.color.p2p_call_background_color
        set(value) {
            field = value
            updateState(field, value, BR.p2pCallBackgroundColor)
        }

    private fun updateState(field: Any, value: Any, fieldId: Int) {
        if (field == value) {
            notifyPropertyChanged(fieldId)
        }
    }


    fun updateUiState(state: UIState) {
        val voipState = PrefManager.getVoipState()

        this.isStartGameClicked = state.isStartGameClicked

        this.isMute = state.isOnMute

        this.topic = state.topicName

        this.type = state.callType

        this.topicImage = state.currentTopicImage

        this.aspiration = state.aspiration

        this.occupation = state.occupation

        this.isSpeakerOn = state.isSpeakerOn

        this.isRemoteUserMuted = state.isRemoteUserMuted

        this.startTime = state.startTime

        this.recordingButtonState = state.recordingButtonState

        this.recordTime = state.recordingStartTime

        this.isRecordingEnabled = state.isRecordingEnabled

        this.isCallerSpeaking = state.isCallerSpeaking

        this.gameWord = state.nextGameWord

        this.wordColor = state.nextGameWordColor

        this.isStartGameClicked = state.isStartGameClicked

        this.isNextWordClicked = state.isNextWordClicked

        this.topic = state.topicName

        this.topicImage = state.currentTopicImage

        this.type = state.callType

        this.isSpeakerOn = state.isSpeakerOn

        this.isMute = state.isOnMute


        if(state.occupation!=this.occupation)
        this.occupation = getOccupationText(state.aspiration, state.occupation)

        if(voipState!=State.IDLE && voipState != State.SEARCHING) {
            this.name = state.remoteUserName
            this.profileImage = state.remoteUserImage ?: ""
        }

        try {
            this.localUserName = Utils.context?.getMentorName()?:""
            this.localUserProfile = Utils.context?.getMentorProfile()?:""
        }catch (ex:Exception){}

        this.title = when (state.callType) {
            PEER_TO_PEER -> "Practice with Partner"
            FPP -> "Favorite Practice Partner"
            GROUP -> "Group Call"
            else -> ""
        }

        if (this.gameWord == "") {
            this.p2pCallBackgroundColor = R.color.p2p_call_background_color
        } else {
            this.p2pCallBackgroundColor = R.color.black
        }
    }

    private fun getOccupationText(aspiration: String, occupation: String): String {
        if (!checkIfNullOrEmpty(occupation) && !checkIfNullOrEmpty(aspiration)) {
            return "$occupation, Dream - $aspiration"
        } else if (checkIfNullOrEmpty(occupation) && !checkIfNullOrEmpty(aspiration)) {
            return "Dream - $aspiration"
        } else if (!checkIfNullOrEmpty(occupation) && checkIfNullOrEmpty(aspiration)) {
            return occupation
        }
        return ""
    }

    private fun checkIfNullOrEmpty(word: String): Boolean {
        return word == "" || word == "null"
    }
}


