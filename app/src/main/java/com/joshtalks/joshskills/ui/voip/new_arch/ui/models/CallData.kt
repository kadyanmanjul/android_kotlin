package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.joshtalks.joshskills.BR

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

    @get:Bindable
    var profileImage : String = ""
    set(value) {
        field = value
        notifyPropertyChanged(BR.profileImage)
    }

    @get:Bindable
    var currentState : String = "Connecting..."
        set(value) {
            field = value
            notifyPropertyChanged(BR.currentState)
        }

    @get:Bindable
    var name : String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.name)
        }

    @get:Bindable
    var topic : String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.topic)
        }

    @get:Bindable
    var type : Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.type)
        }

    @get:Bindable
    var title : String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.title)
        }

    @get:Bindable
    var startTime : Long = 0L
        set(value) {
            field = value
            notifyPropertyChanged(BR.startTime)
        }

    @get:Bindable
    var isMute: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.mute)
        }

    @get:Bindable
    var isSpeakerOn: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.speakerOn)
        }

    @get:Bindable
    var isOnHold: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.onHold)
        }

    @get:Bindable
    var isRemoteUserMuted: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.remoteUserMuted)
        }
}