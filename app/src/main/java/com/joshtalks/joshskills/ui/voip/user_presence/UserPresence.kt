package com.joshtalks.joshskills.ui.voip.user_presence

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserPresence():UserPresenceInterface {
    private val userMap:HashMap<String,Any> = HashMap<String,Any>()
    private val pathString:String="OnlineStatus"
    companion object {
        var database: DatabaseReference?=null
    }

    private fun getFirebaseDBReference(){
        if(database==null) {
            synchronized(this) {
                database = FirebaseDatabase.getInstance().reference
            }
        }
    }

    override fun setUserPresenceInDB(key: String, value: PresenceStatus) {
        getFirebaseDBReference()
        userMap[key] = value
        database?.child(pathString)?.updateChildren(userMap)
    }

}