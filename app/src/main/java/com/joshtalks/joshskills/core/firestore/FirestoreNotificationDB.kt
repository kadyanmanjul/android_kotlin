package com.joshtalks.joshskills.core.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.LAST_FIRESTORE_NOTIFICATION_TIME
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.notification.FirebaseNotificationService
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.core.notification.model.NotificationModel
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationAction
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObjectV2
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val COLLECTION_NOTIFICATION = "NotificationsV2"

object FirestoreNotificationDB {

    private val firestore by lazy { Firebase.firestore }
    private val notificationsCollection by lazy { firestore.collection(COLLECTION_NOTIFICATION) }
    private var notificationListener: ListenerRegistration? = null

    fun getNotification(
        mentorId: String = Mentor.getInstance().getId()
    ) {
        notificationsCollection
            .document(mentorId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    querySnapshot.toObject(FirestoreNotificationObjectV2::class.java)?.let {
                        Timber.d("FirestoreNotificationDB : Notification : $it")
                        Log.d("Manjul", "getNotification() FirestoreNotificationDB : Notification : $it")
                        saveCurrentNotificationTime(it.modified!!.seconds)
                        removeNotificationAfterRead(mentorId)
                        CoroutineScope(Dispatchers.IO).launch {
                            val notification = AppObjectController.appDatabase.notificationDao()
                                .getNotification(it.id.toString())
                            if (notification!=null){

                            } else {
                                val nc = it.toNotificationObject(it.id.toString())
                                /*if (nc.action == NotificationAction.INCOMING_CALL_NOTIFICATION)
                                    nc.actionData?.let { VoipAnalytics.pushIncomingCallAnalytics(it) }*/
                                    AppObjectController.appDatabase.notificationDao().insertNotification(
                                        NotificationModel(
                                            nc.id.toString(),
                                            "firestore",
                                            System.currentTimeMillis(),
                                            System.currentTimeMillis(),
                                            "recieved",
                                            0L
                                        )
                                    )
                                FirebaseNotificationService.sendFirestoreNotification(nc,AppObjectController.joshApplication)
                            }
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
        listener: NotificationListener
    ) {
        notificationListener?.remove()
        try {
            notificationListener = notificationsCollection
                .document(mentorId)
                .addSnapshotListener { querySnapshot, error ->
                    if (querySnapshot != null && error == null) {
                        try {
                            if (querySnapshot.metadata.isFromCache) {
                                Timber.d("FirestoreNotificationDB : NotificationListener : Cached data")
                                return@addSnapshotListener
                            }
                            querySnapshot.toObject(FirestoreNotificationObjectV2::class.java)?.let {
                                    Timber.d("FirestoreNotificationDB : NotificationListener : $it")
                                    saveCurrentNotificationTime(it.modified!!.seconds)
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
                    Timber.d("FirestoreNotificationDB : Notification deleted!")
                }
                .addOnFailureListener { error ->
                    Timber.e(error, "FirestoreNotificationDB : Error deleting notification")
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
    fun onReceived(firestoreNotification: FirestoreNotificationObjectV2)
}
