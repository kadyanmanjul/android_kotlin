package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

interface CallData{
    fun getProfileImage(): String?
    fun getCallerName():String
    fun getTopicHeader():String
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