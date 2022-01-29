package com.joshtalks.joshskills.quizgame.ui.data.network

import com.google.firebase.firestore.*
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.repository.local.model.Mentor

class GameFirebaseDatabase {

    private var database = FirebaseFirestore.getInstance()
    private var collectionCurrentUserRoomId: CollectionReference = database.collection("UserRoom")
    private var animShow: CollectionReference = database.collection("ShowAnim")
    private var opponentAnimShow: CollectionReference = database.collection("OpponentShowAnim")
    private var randomUser: CollectionReference = database.collection("UserRoom")
    private var partnerShowCutCard: CollectionReference = database.collection("PartnerShowCut")
    private var opponentShowCutCard: CollectionReference = database.collection("OpponentShowCut")
    private var sentFriendRequest: CollectionReference = database.collection(FRIEND_REQUEST)
    private var acceptFriendRequest : CollectionReference = database.collection("AcceptFppRequest")
    private var userPlayAgain: CollectionReference = database.collection("UserPlay")
    private var playAgainNotification: CollectionReference = database.collection("PlayAgain")
    private var muteUnmute: CollectionReference = database.collection("MuteUnmute")
    private var changeTime: CollectionReference = database.collection("RoomTime")


    //var mentorId : String = Mentor.getInstance().getUserId()
    var userName: String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo

    var onNotificationTrigger: OnNotificationTrigger? = null
    var onRandomUserTrigger: OnRandomUserTrigger? = null
    var onAnimationTrigger: OnAnimationTrigger? = null

    fun getCurrentUserRoomId(mentorId: String, onNotificationTrigger: OnNotificationTrigger) {
        this.onNotificationTrigger = onNotificationTrigger
        var currentUserRoomId: String? = null
        collectionCurrentUserRoomId
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            currentUserRoomId = doc.data["room_id"].toString()
                            onNotificationTrigger.onGetRoomId(currentUserRoomId, mentorId)
                        }
                    }
                }
            }
    }

    fun deleteRoomData(mentorId: String) {
        collectionCurrentUserRoomId.document(mentorId).delete()
    }

    fun createShowAnimForAnotherUser(
        partnerId: String?,
        isCorrect: String,
        choiceAnswer: String,
        marks: String
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["isCorrect"] = isCorrect
        channel["choiceAnswer"] = choiceAnswer
        channel["marks"] = marks
        animShow.document(partnerId ?: "").set(channel)
    }

    fun getAnimShow(mentorId: String, onNotificationTrigger: OnNotificationTrigger) {
        this.onNotificationTrigger = onNotificationTrigger
        animShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val isCorrect = doc.data["isCorrect"].toString()
                            val choiceAnswer = doc.data["choiceAnswer"].toString()
                            val marks = doc.data["marks"].toString()
                            onNotificationTrigger.onShowAnim(
                                mentorId,
                                isCorrect,
                                choiceAnswer,
                                marks
                            )
                        }
                    }
                }
            }
    }

    fun getRandomUserId(mentorId: String, onRandomUserTrigger: OnRandomUserTrigger) {
        this.onRandomUserTrigger = onRandomUserTrigger
        try {
            randomUser
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    for (doc in value!!) {
                        if (doc.exists()) {
                            if (mentorId == doc.id) {
                                val roomId = doc.data["room_id"].toString()
                                onRandomUserTrigger.onSearchUserIdFetch(roomId)
                            }
                        }
                    }
                }
        } catch (ex: Exception) {

        }
    }

    fun deleteAnimUser(partnerId: String) {
        animShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (partnerId == doc.id) {
                            animShow.document(partnerId).delete()
                        }
                    }
                }
            }
    }

    fun createShowAnimForOpponentTeam(opponentTeamId: String?, correct: String?, marks: String?) {
        val channel: HashMap<String, Any> = HashMap()
        channel["isCorrect"] = correct ?: ""
        channel["marks"] = marks ?: ""
        opponentAnimShow.document(opponentTeamId ?: "").set(channel)
    }

    fun getOpponentShowAnim(teamId: String, onAnimationTrigger: OnAnimationTrigger) {
        this.onAnimationTrigger = onAnimationTrigger
        opponentAnimShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id) {
                            val isCorrect = doc.data["isCorrect"].toString()
                            val marks = doc.data["marks"].toString()
                            onAnimationTrigger.onOpponentShowAnim(teamId, isCorrect, marks)
                        }
                    }
                }
            }
    }

    fun deleteOpponentAnimTeam(teamId: String) {
        opponentAnimShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id) {
                            opponentAnimShow.document(teamId).delete()
                        }
                    }
                }
            }
    }

    fun createPartnerShowCutCard(currentUserTeamId: String, correct: String, choiceAnswer: String) {
        val channel: HashMap<String, Any> = HashMap()
        channel["isCorrect"] = correct
        channel["choiceAnswer"] = choiceAnswer
        partnerShowCutCard.document(currentUserTeamId).set(channel)
    }

    fun getPartnerCutCard(teamId: String, onAnimationTrigger: OnAnimationTrigger) {
        this.onAnimationTrigger = onAnimationTrigger
        partnerShowCutCard.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (teamId == doc.id) {
                        val isCorrect = doc.data["isCorrect"].toString()
                        val choiceAnswer = doc.data["choiceAnswer"].toString()
                        onAnimationTrigger.onOpponentPartnerCut(
                            teamId,
                            isCorrect,
                            choiceAnswer
                        )
                    }
                }
            }
        }
    }

    fun deletePartnerCutCard(teamId: String) {
        partnerShowCutCard
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id) {
                            partnerShowCutCard.document(teamId).delete()
                        }
                    }
                }
            }
    }

    fun createOpponentTeamShowCutCard(
        opponentTeamId: String,
        correct: String,
        choiceAnswer: String
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["isCorrect"] = correct
        channel["choiceAnswer"] = choiceAnswer
        opponentShowCutCard.document(opponentTeamId).set(channel)
    }

    fun getOpponentCutCard(opponentTeamId: String, onAnimationTrigger: OnAnimationTrigger) {
        this.onAnimationTrigger = onAnimationTrigger
        opponentShowCutCard
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (opponentTeamId == doc.id) {
                            val isCorrect = doc.data["isCorrect"].toString()
                            val choiceAnswer = doc.data["choiceAnswer"].toString()
                            onAnimationTrigger.onOpponentTeamCutCard(
                                opponentTeamId,
                                isCorrect,
                                choiceAnswer
                            )
                        }
                    }
                }
            }
    }

    fun deleteOpponentCutCard(teamId: String) {
        opponentShowCutCard
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id) {
                            opponentShowCutCard.document(teamId).delete()
                        }
                    }
                }
            }
    }

//    fun deleteTimeChange(mentorId: String) {
//        changeTime.document(mentorId).delete()
//    }

    fun createFriendRequest(
        fromMentorId: String,
        fromUserName: String,
        fromImageUrl: String,
        toUserId: String
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["fromUserId"] = fromMentorId
        channel["fromUserName"] = fromUserName
        channel["fromImageUrl"] = fromImageUrl
        channel["timestamp"] = FieldValue.serverTimestamp()
        channel["isAccept"] = "false"
        sentFriendRequest.document(toUserId).set(channel)
    }

    fun getFriendRequests(toUserId: String, onMakeFriendTrigger: OnMakeFriendTrigger) {
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

    fun createAcceptFriendRequest(acceptedUserId:String, userName:String,userImage: String,fromMentorId: String,isAccept: String){
        val channel: HashMap<String, Any> = HashMap()
        channel["acceptUserId"] = acceptedUserId
        channel["acceptUserName"] = userName
        channel["acceptUserImage"] = userImage
        channel["isAccept"] = isAccept
        acceptFriendRequest.document(fromMentorId).set(channel)
    }

    fun getAcceptFriendRequest(mentorId: String,onMakeFriendTrigger: OnMakeFriendTrigger){
        acceptFriendRequest
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val acceptUserId = doc.data["acceptUserId"].toString()
                            val acceptUserName = doc.data["acceptUserName"].toString()
                            val acceptImage = doc.data["acceptUserImage"].toString()
                            val isAccept = doc.data["isAccept"].toString()
                            onMakeFriendTrigger.onPartnerAcceptFriendRequest(acceptUserName,acceptImage,isAccept)
                        }
                    }
                }
            }
    }

    fun getLiveStatus(mentorId: String): String {
        var status: String? = null
        sentFriendRequest
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            status = doc.data["status"].toString()
                        }
                    }
                }
            }
        return status ?: ""
    }

    fun createPlayAgainNotification(partnerUserId: String, userName: String, userImage: String) {
        val channel: HashMap<String, Any> = HashMap()
        channel["userName"] = userName
        channel["userImage"] = userImage
        playAgainNotification.document(partnerUserId).set(channel)
    }

    fun getPartnerPlayAgainNotification(
        mentorId: String,
        onMakeFriendTrigger: OnMakeFriendTrigger
    ) {
        playAgainNotification
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val userImage = doc.data["userImage"].toString()
                            val userName = doc.data["userName"].toString()
                            onMakeFriendTrigger.onPartnerPlayAgainNotification(
                                userName,
                                userImage,
                                mentorId
                            )
                        }
                    }
                }
            }
    }

    fun getPlayAgainAPiData(mentorId: String, onMakeFriendTrigger: OnMakeFriendTrigger) {
        userPlayAgain
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val userImage = doc.data["image"].toString()
                            val userName = doc.data["name"].toString()
                            onMakeFriendTrigger.onPlayAgainNotificationFromApi(userName, userImage)
                        }
                    }
                }
            }
    }

    fun deleteUserPlayAgainCollection(mentorId: String) {
        userPlayAgain.document(mentorId).delete()
    }

    fun deletePlayAgainNotification(mentorId: String) {
        playAgainNotification.document(mentorId).delete()
    }

    fun deleteAcceptFppRequestNotification(mentorId: String) {
        acceptFriendRequest.document(mentorId).delete()
    }

    fun deleteAllData(mentorId: String) {
        sentFriendRequest.document(mentorId).delete()
    }

    fun createMicOnOff(partnerId: String, isMute: String) {
        val channel: HashMap<String, Any> = HashMap()
        channel["partnerId"] = partnerId
        channel["isMute"] = isMute
        muteUnmute.document(partnerId).set(channel)
    }

    fun getMuteOrUnMute(mentorId: String, onAnimationTrigger: OnAnimationTrigger) {
        muteUnmute
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            val status = doc.data["isMute"].toString()
                            onAnimationTrigger.onMicOnOff(mentorId, status)
                        }
                    }
                }
            }
    }

    fun getRoomTime(roomId: String, onTimeChange: OnTimeChange) {
        changeTime.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (roomId == doc.id) {
                        onTimeChange.onTimeChangeMethod(doc.data["time"] as Long)
                    }
                }
            }
        }
    }

    fun deleteMuteUnmute(mentorId: String) {
        muteUnmute.document(mentorId).delete()
    }

    fun deleteChange(mentorId: String) {
        changeTime.document(mentorId).delete()
    }

    interface OnNotificationTrigger {
        fun onGetRoomId(currentUserRoomID: String?, mentorId: String)
        fun onShowAnim(mentorId: String, isCorrect: String, choiceAnswer: String, marks: String)
    }

    interface OnRandomUserTrigger {
        fun onSearchUserIdFetch(roomId: String)
    }

    interface OnAnimationTrigger {
        fun onOpponentShowAnim(opponentTeamId: String?, isCorrect: String, marks: String)
        fun onOpponentPartnerCut(teamId: String, isCorrect: String, choiceAnswer: String)
        fun onOpponentTeamCutCard(opponentTeamId: String, isCorrect: String, choiceAnswer: String)
        fun onMicOnOff(partnerUserId: String, status: String)
    }

    interface OnMakeFriendTrigger {
        fun onSentFriendRequest(
            fromMentorId: String,
            fromUserName: String,
            fromImageUrl: String,
            isAccept: String
        )

        fun onPlayAgainNotificationFromApi(userName: String, userImage: String)
        fun onPartnerPlayAgainNotification(userName: String, userImage: String, mentorId: String)
        fun onPartnerAcceptFriendRequest(userName: String,userImage: String,isAccept: String)
    }

    interface OnLiveStatus {
        fun onGetLiveStatus(status: String, mentorId: String)
    }

    interface OnTimeChange {
        fun onTimeChangeMethod(time: Long)
    }

    interface OnTimeUpdate {
        fun onTimeUpdateMethod(time: Long)
    }
}