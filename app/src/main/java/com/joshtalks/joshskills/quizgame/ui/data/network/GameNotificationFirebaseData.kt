package com.joshtalks.joshskills.quizgame.ui.data.network

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.google.android.gms.tasks.OnCompleteListener

const val REQUEST_NOTIFICATION = "RequestTemp"
const val REQUEST_DECLINE = "RequestDecline"
const val USER_STATUS = "UserStatus"
const val ACCEPT_REQUEST = "Accept_Request"
const val FRIEND_REQUEST = "FriendRequest"


class GameNotificationFirebaseData {
    private var database = FirebaseFirestore.getInstance()

    private var collectionReference: CollectionReference = database.collection(REQUEST_NOTIFICATION)
    private var requestDecline: CollectionReference = database.collection(REQUEST_DECLINE)
    private var statusCollection: CollectionReference = database.collection(USER_STATUS)
    private var acceptRequestCollection: CollectionReference = database.collection(ACCEPT_REQUEST)
    private var sentFriendRequest: CollectionReference = database.collection(FRIEND_REQUEST)

    var userName: String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo

    var onNotificationTriggerTemp:OnNotificationTriggerTemp?=null
    fun createRequest(favUserId: String?, channelName: String?, mentorId: String) {
        val channel: HashMap<String, Any> = HashMap()
        channel["fromUserId"] = mentorId
        channel["fromUserName"] = userName ?: ""
        channel["fromImageUrl"] = imageUrl ?: ""
        channel["channelName"] = channelName ?: ""
        channel["timestamp"] = FieldValue.serverTimestamp()
        channel["isAccept"] = "false"
        collectionReference.document(favUserId ?: "").set(channel)
    }

    fun getUserDataFromFirestore(
        mentorId: String,
        onNotificationTrigger: OnNotificationTriggerTemp
    ) {
        this.onNotificationTriggerTemp = onNotificationTrigger
        try {
            collectionReference.addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val channelName: String = doc.data["channelName"].toString()
                            val fromUserId = doc.data["fromUserId"].toString()
                            val fromUserName = doc.data["fromUserName"].toString()
                            val fromImageUrl = doc.data["fromImageUrl"]?.toString()
                            onNotificationTriggerTemp?.onNotificationForInvitePartnerTemp(
                                channelName,
                                fromUserId,
                                fromUserName,
                                fromImageUrl ?: ""
                            )
                        }
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun deleteUserData(mentorId: String) {
        collectionReference.document(mentorId).delete()
    }

    fun getAcceptCall(mentorId: String, onNotificationTrigger123: OnNotificationTriggerTemp) {
        acceptRequestCollection
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val channelName = doc.data["channelName"].toString()
                            val timestamp = doc.data["timestamp"].toString()
                            val isAccept = doc.data["isAccept"].toString()
                            val opponentMemberId = doc.data["opponentMemberId"].toString()
                            val mentorIdIdAcceptedUser = doc.data["mentorId"].toString()
                            if (isAccept == "true") {
                                onNotificationTrigger123.onNotificationForPartnerAcceptTemp(
                                    channelName,
                                    timestamp,
                                    isAccept,
                                    opponentMemberId,
                                    mentorIdIdAcceptedUser
                                )
                            }
                        }
                    }
                }
            }
    }

    fun deleteRequested(mentorId: String) {
        var mId: String? = mentorId
        val cr: CollectionReference = database.collection(REQUEST_NOTIFICATION)
        cr.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (mId == doc.id) {
                        cr.document(mId ?: "").delete().addOnCompleteListener(
                            OnCompleteListener {
                                mId = ""
                            })
                    }
                }
            }
        }
    }

    fun deleteDeclineData(mentorId: String) {
        var mId: String? = mentorId
        //val requestD: CollectionReference = database.collection(REQUEST_DECLINE)
        requestDecline
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mId == doc.id) {
                            requestDecline.document(mId ?: "").delete().addOnCompleteListener(
                                OnCompleteListener {
                                    mId = null
                                })
                        }
                    }
                }
            }
    }

    fun createRequestDecline(
        fromUserId: String,
        declineUserName: String?,
        declineUserImage: String?,
        declinedUserId: String?
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["declineUserName"] = declineUserName ?: ""
        channel["declineUserImage"] = declineUserImage ?: ""
        channel["declineUserId"] = declinedUserId ?: ""
        requestDecline.document(fromUserId).set(channel)
    }

    fun getDeclineCall(mentorId: String, onNotificationTrigger1: OnNotificationTriggerTemp) {
        val mID: String = mentorId
        // val rD: CollectionReference = database.collection(REQUEST_DECLINE)
        requestDecline.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (mID == doc.id) {
                        val declinedUserName = doc.data["declineUserName"].toString()
                        val declinedUserImage = doc.data["declineUserImage"].toString()
                        val declinedUserId = doc.data["declineUserId"].toString()
                        onNotificationTrigger1.onNotificationForPartnerNotAcceptTemp(
                            declinedUserName,
                            declinedUserImage,
                            mentorId,
                            declinedUserId
                        )
                    }
                }
            }
        }
    }

    fun deleteAllData(mentorId: String) {
        collectionReference.document(mentorId).delete()
        requestDecline.document(mentorId).delete()
        acceptRequestCollection.document(mentorId).delete()
        sentFriendRequest.document(mentorId).delete()
    }

    fun statusLive(mentorId: String, onLiveStatus: GameFirebaseDatabase.OnLiveStatus) {
        statusCollection
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val status = doc.data["status"].toString()
                            onLiveStatus.onGetLiveStatus(status, mentorId)
                        }
                    }
                }
            }
    }

    fun changeUserStatus(mentorId: String, status: String) {
        val channel: HashMap<String, Any> = HashMap()
        channel["status"] = status
        statusCollection.document(mentorId).set(channel)
    }

    fun acceptRequest(
        opponentMemberId: String,
        isAccept: String,
        opponentMemberName: String,
        channelName: String,
        mentorId: String
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["mentorId"] = mentorId
        channel["opponentMemberId"] = opponentMemberId
        channel["opponentMemberName"] = opponentMemberName
        channel["channelName"] = channelName
        channel["timestamp"] = FieldValue.serverTimestamp()
        channel["isAccept"] = isAccept
        acceptRequestCollection.document(opponentMemberId).set(channel)
    }

    fun deleteDataAcceptRequest(mentorId: String) {
        acceptRequestCollection.document(mentorId).delete()
    }

    fun getFriendRequests(
        toUserId: String,
        onMakeFriendTrigger: GameFirebaseDatabase.OnMakeFriendTrigger
    ) {
        sentFriendRequest
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (toUserId == doc.id) {
                            val fromUserId = doc.data["fromUserId"].toString()
                            val fromUserName = doc.data["fromUserName"].toString()
                            val fromImageUrl = doc.data["fromImageUrl"].toString()
                            val isAccept = doc.data["isAccept"].toString()
                            onMakeFriendTrigger.onSentFriendRequest(
                                fromUserId,
                                fromUserName,
                                fromImageUrl,
                                isAccept
                            )
                        }
                    }
                }
            }
    }

    fun deleteRequest(mentorId: String) {
        sentFriendRequest.document(mentorId).delete()
    }

    interface OnNotificationTriggerTemp {
        fun onNotificationForInvitePartnerTemp(
            channelName: String,
            fromUserId: String,
            fromUserName: String,
            fromUserImage: String
        )

        fun onNotificationForPartnerNotAcceptTemp(
            userName: String?,
            userImageUrl: String,
            fromUserId: String,
            declinedUserId: String
        )

        fun onNotificationForPartnerAcceptTemp(
            channelName: String?,
            timeStamp: String,
            isAccept: String,
            opponentMemberId: String,
            mentorId: String
        )
    }
}