package com.joshtalks.joshskills.ui.voip.presence

import com.google.firebase.database.FirebaseDatabase

object UserPresence : UserPresenceInterface {
    private val userStatusMap:HashMap<String,Any?> = HashMap()
    private const val pathString:String="OnlineStatus"
    private var time:Long?=null

    private val databaseRef by lazy {
        FirebaseDatabase.getInstance().reference
    }

    override fun setUserPresence(key: String, timeStamp: Long?) {
        if(timeStamp!=null){
            time=timeStamp
        }
        userStatusMap[key] = time
        databaseRef.child(pathString).updateChildren(userStatusMap)
    }
}