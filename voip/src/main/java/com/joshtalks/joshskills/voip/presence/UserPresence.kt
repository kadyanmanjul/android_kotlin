package com.joshtalks.joshskills.voip.presence

import com.google.firebase.database.FirebaseDatabase
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.VOIP_DB_URL
import com.joshtalks.joshskills.voip.constant.VOIP_PATH
import com.joshtalks.joshskills.voip.voipLog

object UserPresence : UserPresenceInterface {
    private val userStatusMap:HashMap<String,Any?> = HashMap()
    private val pathString:String= VOIP_PATH

    private val databaseRef by lazy {
        FirebaseDatabase.getInstance(VOIP_DB_URL).reference.child(pathString)
    }
    init {
        setOnDisconnect()
    }

    private fun setOnDisconnect() {
        userStatusMap.clear()
        userStatusMap[Utils.uuid.toString()] = false
        databaseRef.onDisconnect().setValue(userStatusMap).addOnSuccessListener(){
            voipLog?.log("OnSuccessListener $it")
        }.addOnFailureListener {
           voipLog?.log("addOnFailureListener $it")
        }
    }

    override fun setUserPresence(key: String, value: PresenceStatus) {
        userStatusMap.clear()
        when(value){
            PresenceStatus.Offline ->{
                userStatusMap[key] = false
            }
            PresenceStatus.Online -> {
                userStatusMap[key] = true
            }
        }
        databaseRef.updateChildren(userStatusMap).addOnSuccessListener {
            voipLog?.log("OnSuccessListener $it")
        }.addOnFailureListener {
            voipLog?.log("OnSuccessListener $it")
        }
    }

}