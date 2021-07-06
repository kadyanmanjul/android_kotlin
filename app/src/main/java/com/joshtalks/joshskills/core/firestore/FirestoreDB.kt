package com.joshtalks.joshskills.core.firestore

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.repository.local.model.Mentor

const val COLLECTION_AGORA_USER_STATE = "AgoraUserState"
const val COLLECTION_AGORA_CHANNEL_STATE = "AgoraChannelState"
const val COLLECTION_AGORA_NOTIFICATION = "Notifications"

object FirestoreDB {

    private val firestore by lazy { Firebase.firestore }
    private val notificationsCollection by lazy { firestore.collection(COLLECTION_AGORA_NOTIFICATION) }

    fun getNotification(
        mentorId: String = Mentor.getInstance().getId(),
        onSuccess: (FirestoreNotificationObject) -> Unit
    ) {
        notificationsCollection
            .document(mentorId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    querySnapshot.toObject(FirestoreNotificationObject::class.java)?.let {
                        onSuccess(it)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
    }
}
