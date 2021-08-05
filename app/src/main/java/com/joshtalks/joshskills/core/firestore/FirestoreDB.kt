package com.joshtalks.joshskills.core.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.core.LAST_FIRESTORE_NOTIFICATION_TIME
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationAction
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
                        if (isNotificationLatest(it)) {
                            Timber.d("FSDB : Notification : $it")
                            saveCurrentNotificationTime(it.modified!!.seconds)
                            onSuccess(it)
                            // removeNotificationAfterRead(mentorId)
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
    }

    private fun saveCurrentNotificationTime(timeInSec: Long) {
        PrefManager.put(LAST_FIRESTORE_NOTIFICATION_TIME, timeInSec)
    }

    fun setNotificationListener(
        mentorId: String = Mentor.getInstance().getId(),
        listener: AgoraNotificationListener
    ) {
        notificationListener?.remove()
        try {
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
                                if (it.action != FirestoreNotificationAction.CALL_RECEIVE_NOTIFICATION &&
                                    isNotificationLatest(it)
                                ) {
                                    Timber.d("FSDB : NotificationListener : $it")
                                    saveCurrentNotificationTime(it.modified!!.seconds)
                                    listener.onReceived(it)
                                    // removeNotificationAfterRead(mentorId)
                                }
                            }
                        } catch (ex: Exception) {
                            Timber.w(ex)
                        }
                    }
                }
        } catch (ex: Exception) {
            Timber.w(ex)
        }
    }

    private fun isNotificationLatest(obj: FirestoreNotificationObject): Boolean {
        val lastFSNotificationTime = PrefManager.getLongValue(LAST_FIRESTORE_NOTIFICATION_TIME)
        return when {
            obj.modified == null -> {
                false
            }
            lastFSNotificationTime > 0 -> {
                obj.modified!!.seconds > lastFSNotificationTime
            }
            else -> {
                obj.modified!!.seconds > Timestamp.now().seconds - 300
            }
        }
    }

    fun removeNotificationAfterRead(mentorId: String = Mentor.getInstance().getId()) {
        try {
            notificationsCollection
                .document(mentorId)
                .delete()
                .addOnSuccessListener {
                    Timber.d("FSDB : Notification deleted!")
                }
                .addOnFailureListener { error ->
                    Timber.e(error, "FSDB : Error deleting notification")
                }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }
}

interface AgoraNotificationListener {
    fun onReceived(firestoreNotification: FirestoreNotificationObject)
}
