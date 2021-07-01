package com.joshtalks.joshskills.core.firestore

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.repository.local.model.Mentor
import timber.log.Timber

const val COLLECTION_AGORA_NOTIFICATION = "Notifications"

object FirestoreDB {

    private val firestore by lazy { Firebase.firestore }
    private val notificationsCollection by lazy { firestore.collection(COLLECTION_AGORA_NOTIFICATION) }
    private var notificationListener: ListenerRegistration? = null

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
                        Timber.d("FSDB : Notification : $it")
                        onSuccess(it)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
    }

    fun setNotificationListener(
        mentorId: String = Mentor.getInstance().getId(),
        listener: AgoraNotificationListener
    ) {
        notificationListener?.remove()
        notificationListener = notificationsCollection
            .document(mentorId)
            .addSnapshotListener { querySnapshot, error ->
                if (querySnapshot != null && error == null) {
                    try {
                        if (querySnapshot.metadata.isFromCache) {
                            Timber.d("FSDB : NotificationListener : Cached data")
                            return@addSnapshotListener
                        }
                        querySnapshot.toObject(FirestoreNotificationObject::class.java)?.let {
                            Timber.d("FSDB : NotificationListener : $it")
                            listener.onReceived(it)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
    }
}

interface AgoraNotificationListener {
    fun onReceived(firestoreNotification: FirestoreNotificationObject)
}
