package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

interface CallData{
    fun getProfileImage():String
    fun getCallerName():String
    fun getTopicHeader():String
    fun getTopicName():String
    fun getCallType(): CallType
    fun getCallTypeHeader():String
}
sealed class CallType{
    object FavoritePracticePartner:CallType()
    object NormalPracticePartner:CallType()
    object GroupCall:CallType()
}