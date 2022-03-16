package com.joshtalks.joshskills.voip.presence

import com.google.firebase.database.FirebaseDatabase

class UserPresence : UserPresenceInterface {
    private val userMap:HashMap<String,Any> = HashMap()
    private val pathString:String="OnlineStatus"
    private var status:Boolean=false
    private val database by lazy {
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
        userMap[key] = status
        database?.child(pathString)?.updateChildren(userMap)
    }

}