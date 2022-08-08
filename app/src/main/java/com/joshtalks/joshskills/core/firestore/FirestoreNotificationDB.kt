package com.joshtalks.joshskills.core.firestore

import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.repository.local.model.FirestoreNewNotificationObject
import com.joshtalks.joshskills.repository.local.model.Mentor
import timber.log.Timber

const val COLLECTION_NOTIFICATION = "NotificationsV2"
private const val TAG = "FirestoreNotificationDB"

object FirestoreNotificationDB {

    private val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(false)
        .build()
    private val firestore by lazy {
        Firebase.firestore.apply { firestoreSettings = settings }
    }
    private val notificationsCollection by lazy { firestore.collection(COLLECTION_NOTIFICATION) }
    private var notificationListener: ListenerRegistration? = null

    fun setNotificationListener(
        mentorId: String = Mentor.getInstance().getId(),
        listener: NotificationListener
    ) {
        notificationListener?.remove()
        if (mentorId.isBlank()){
            return
        }
        try {
            notificationListener = notificationsCollection
                .document(mentorId)
                .addSnapshotListener { querySnapshot, error ->
                    Timber.d("$TAG addSnapshotListener called querySnapshot : ${querySnapshot} error = $error")
                    if (querySnapshot != null && error == null) {
                        try {
                            if (querySnapshot.metadata.isFromCache) {
                                Timber.d("$TAG : NotificationListener : Cached data")
                                return@addSnapshotListener
                            }
                            querySnapshot.toObject(FirestoreNewNotificationObject::class.java)?.let {
                                    Timber.d("$TAG : NotificationListener : $it")
                                    listener.onReceived(it)
                                    removeNotificationAfterRead(mentorId)
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

    fun removeNotificationAfterRead(mentorId: String = Mentor.getInstance().getId()) {
        try {
            notificationsCollection
                .document(mentorId)
                .delete()
                .addOnSuccessListener {
                    Timber.d("$TAG : Notification deleted!")
                }
                .addOnFailureListener { error ->
                    Timber.e(error, "$TAG : Error deleting notification")
                }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    fun unsubscribe() {
        notificationListener?.remove()
    }
}

interface NotificationListener {
    fun onReceived(firestoreNewNotification: FirestoreNewNotificationObject)
}
