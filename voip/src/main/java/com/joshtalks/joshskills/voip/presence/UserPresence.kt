package com.joshtalks.joshskills.voip.presence

import com.google.firebase.database.FirebaseDatabase

object UserPresence : UserPresenceInterface {
    private val userStatusMap:HashMap<String,Any> = HashMap()
    private val pathString:String="OnlineStatus"
    private var status:Boolean=false
    private val databaseRef by lazy {
        FirebaseDatabase.getInstance().reference
    }

    override fun setUserPresence(key: String, value: PresenceStatus) {
        status = when(value){
            PresenceStatus.Online ->{
                true
            }
            PresenceStatus.Offline ->{
                false
            }
        }
        userStatusMap[key] = status
        databaseRef.child(pathString).updateChildren(userStatusMap)
    }
}