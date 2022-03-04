package com.joshtalks.joshskills.ui.voip.user_presence

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserPresence :UserPresenceInterface {
    private val userMap:HashMap<String,Any> = HashMap()
    private val pathString:String="OnlineStatus"
    private var status:Boolean=false

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

    override fun setUserPresence(key: String, value: PresenceStatus) {
        status = when(value){
            PresenceStatus.Online->{
                true
            }
            PresenceStatus.Offline->{
                false
            }
        }
        getFirebaseDBReference()
        userMap[key] = status
        database?.child(pathString)?.updateChildren(userMap)
    }

}