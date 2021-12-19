package com.joshtalks.joshskills.quizgame.ui.data.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.*
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.repository.local.model.Mentor

class FirebaseDatabase {

    private var database = FirebaseFirestore.getInstance()
    private var collectionReference: CollectionReference = database.collection("Request")
    private var acceptRequestCollection: CollectionReference = database.collection("Accept_Request")

    private var collectionCurrentUserRoomId: CollectionReference = database.collection("UserRoom")
    private var requestDecline: CollectionReference = database.collection("request_decline")
    private var animShow: CollectionReference = database.collection("ShowAnim")

    private var opponentAnimShow: CollectionReference = database.collection("OpponentShowAnim")

    private var randomUser: CollectionReference = database.collection("UserRoom")

    private var partnerShowCutCard: CollectionReference = database.collection("PartnerShowCut")
    private var opponentShowCutCard: CollectionReference = database.collection("OpponentShowCut")

    private var sentFriendRequest: CollectionReference = database.collection("FriendRequest")

    private var statuCollection :CollectionReference = database.collection("UserStatus")

    private var liveStatus: CollectionReference = database.collection("UserStatus")
    private var userPlayAgain: CollectionReference = database.collection("UserPlay")
    private var  playAgainNotification : CollectionReference = database.collection("PlayAgain")
    private var muteUnmute :CollectionReference = database.collection("MuteUnmute")


    //var mentorId : String = Mentor.getInstance().getUserId()
    var userName: String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo

    var onNotificationTrigger: OnNotificationTrigger? = null
    var onRandomUserTrigger: OnRandomUserTrigger? = null
    var onAnimationTrigger: OnAnimationTrigger? = null


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

    fun getUserDataFromFirestore(mentorId: String, onNotificationTrigger1: OnNotificationTrigger) {
        try {
                val cr: CollectionReference = database.collection("Request")
                cr.addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    for (doc in value!!) {
                        if (doc.exists()) {
                            if (mentorId == doc.id) {
                                var channelName: String = doc.data["channelName"].toString()
                                var fromUserId = doc.data["fromUserId"].toString()
                                var fromUserName = doc.data["fromUserName"].toString()
                                var fromImageUrl = doc.data["fromImageUrl"]?.toString()
                                onNotificationTrigger1.onNotificationForInvitePartner(
                                    channelName,
                                    fromUserId,
                                    fromUserName,
                                    fromImageUrl ?: ""
                                )
                            }
                        }
                    }
                }
        }catch (ex:Exception){

        }
    }

    fun deleteUserData(mentorId: String, fromUserMentorId: String) {
        val cr: CollectionReference = database.collection("Request")
        var mId: String? = mentorId
        var fUMId: String? = fromUserMentorId
        cr.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (mId == doc.id) {
                        var fromUserId: String? = doc.data["fromUserId"].toString()
                        if (fromUserId == fUMId) {
                            try {
                                    cr.document(mId ?: "").delete().addOnCompleteListener(
                                        OnCompleteListener {
                                            createRequestDecline(fromUserId ?: "", userName, imageUrl,mId?:"")
                                            fromUserId = ""
                                            fUMId = ""
                                            mId = ""
                                        })
                            } catch (ex: Exception) {

                            }
                        }
                    }
                }
            }
        }
    }

//    fun deleteUserDataBy10Sec(mentorId: String, fromUserMentorId: String) {
//        val cr1: CollectionReference = database.collection("Request")
//        var mId: String? = mentorId
//        var fUMId: String? = fromUserMentorId
//        try {
//            cr1.addSnapshotListener { value, e ->
//                if (e != null) {
//                    return@addSnapshotListener
//                }
//                for (doc in value!!) {
//                    if (doc.exists()) {
//                        if (mId == doc.id) {
//                            var fromUserId: String? = doc.data["fromUserId"].toString()
//                            if (fromUserId == fUMId) {
//                                val handler = Handler(Looper.getMainLooper())
//                                try {
//                                    handler.postDelayed({
//                                        cr1.document(mId ?: "").delete().addOnCompleteListener(
//                                            OnCompleteListener {
//                                                createRequestDecline(fromUserId ?: "", userName, imageUrl,mId?:"")
//                                                fromUserId = ""
//                                                fUMId = ""
//                                                mId = ""
//                                            })
//                                    }, 10000)
//                                } catch (ex: Exception) {
//
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//        }catch (ex:Exception){}
//    }

    fun deleteRequested(mentorId: String) {
        var mId: String? = mentorId
        val cr: CollectionReference = database.collection("Request")
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

    fun getAcceptCall(mentorId: String, onNotificationTrigger123: OnNotificationTrigger) {
        var acceptRequestC: CollectionReference = database.collection("Accept_Request")
        var mID: String? = mentorId
        acceptRequestC
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mID == doc.id) {
                            var channelName = doc.data["channelName"].toString()
                            var timestamp = doc.data["timestamp"].toString()
                            var isAccept = doc.data["isAccept"].toString()
                            var opponentMemberId = doc.data["opponentMemberId"].toString()
                            var mentorIdIdAcceptedUser = doc.data["mentorId"].toString()
                            if (isAccept == "true") {
                                onNotificationTrigger123.onNotificationForPartnerAccept(
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

    fun createRequestDecline(
        fromUserId: String,
        declineUserName: String?,
        declineUserImage: String?,
        declinedUserId:String?
    ) {
        val channel: HashMap<String, Any> = HashMap()
        channel["declineUserName"] = declineUserName ?: ""
        channel["declineUserImage"] = declineUserImage ?: ""
        channel["declineUserId"] = declinedUserId ?: ""
        requestDecline.document(fromUserId).set(channel)
    }

    fun getDeclineCall(mentorId: String, onNotificationTrigger1122: OnNotificationTrigger) {
        var mID: String? = mentorId
        var rD: CollectionReference = database.collection("request_decline")
        rD.addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (mID == doc.id) {
                        val declinedUserName = doc.data["declineUserName"].toString()
                        val declinedUserImage = doc.data["declineUserImage"].toString()
                        var declinedUserId = doc.data["declineUserId"].toString()
                        onNotificationTrigger1122.onNotificationForPartnerNotAccept(
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

    fun deleteDeclineData(mentorId: String) {
        var mId: String? = mentorId
        var requestD: CollectionReference = database.collection("request_decline")
        requestD
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mId == doc.id) {
                            requestD.document(mId ?: "").delete().addOnCompleteListener(
                                OnCompleteListener {
                                    mId = null
                                })
                        }
                    }
                }
            }
    }

    fun deleteDataAcceptRequest(mentorId: String) {
        var mId: String? = mentorId
        var acceptRequestC: CollectionReference = database.collection("Accept_Request")
        acceptRequestC
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mId == doc.id) {
                            acceptRequestC.document(mentorId).delete().addOnCompleteListener(
                                OnCompleteListener {
                                    mId = null
                                })
                        }
                    }
                }
            }
    }

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

    fun acceptOrDeclineRequest(mentorId: String, opponentMemberId: String, isAccept: String, ) {

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
        }catch (ex:Exception){

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

    fun getPartnerCutCard(teamId: String,onAnimationTrigger: OnAnimationTrigger) {
        this.onAnimationTrigger = onAnimationTrigger
        partnerShowCutCard
            .addSnapshotListener { value, e ->
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

    fun getOpponentCutCard(opponentTeamId: String,onAnimationTrigger: OnAnimationTrigger) {
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
                            var fromUserId = doc.data["fromUserId"].toString()
                            var fromUserName = doc.data["fromUserName"].toString()
                            var fromImageUrl = doc.data["fromImageUrl"].toString()
                            var isAccept = doc.data["isAccept"].toString()
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

    fun getLiveStatus(mentorId: String) :String{
        var status:String?=null
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
        return status?:""
    }

    fun createPlayAgainNotification(partnerUserId: String,userName: String,userImage: String){
        val channel: HashMap<String, Any> = HashMap()
        channel["userName"] = userName
        channel["userImage"] = userImage
        playAgainNotification.document(partnerUserId).set(channel)
    }

    fun getPartnerPlayAgainNotification(mentorId: String,onMakeFriendTrigger: OnMakeFriendTrigger){
        playAgainNotification
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            var userImage = doc.data["userImage"].toString()
                            var userName = doc.data["userName"].toString()
                            onMakeFriendTrigger.onPartnerPlayAgainNotification(userName,userImage,mentorId)
                        }
                    }
                }
            }
    }

    fun getPlayAgainAPiData(mentorId: String,onMakeFriendTrigger: OnMakeFriendTrigger) {
        userPlayAgain
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            var userImage = doc.data["image"].toString()
                            var userName = doc.data["name"].toString()
                            onMakeFriendTrigger.onPlayAgainNotificationFromApi(userName,userImage)
                        }
                    }
                }
            }
    }

    fun deleteUserPlayAgainCollection(mentorId: String){
        userPlayAgain.document(mentorId).delete()
    }

    fun deletePlayAgainNotification(mentorId: String){
        playAgainNotification.document(mentorId).delete()
    }

    fun deleteAllData(mentorId: String){
        collectionReference.document(mentorId).delete()
        requestDecline.document(mentorId).delete()
        acceptRequestCollection.document(mentorId).delete()
        sentFriendRequest.document(mentorId).delete()
    }

    fun statusLive(mentorId: String,onLiveStatus: OnLiveStatus){
                statuCollection
                    .addSnapshotListener { value, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }
                        for (doc in value!!) {
                            if (doc.exists()) {
                                if (mentorId == doc.id) {
                                    var status = doc.data["status"].toString()
                                    onLiveStatus.onGetLiveStatus(status,mentorId)
                                }
                            }
                        }
                }
    }

    fun createMicOnOff(partnerId: String,isMute: String){
        val channel: HashMap<String, Any> = HashMap()
        channel["partnerId"] = partnerId
        channel["isMute"] = isMute
        muteUnmute.document(partnerId).set(channel)
    }

    fun getMuteOrUnMute(mentorId: String, onAnimationTrigger: OnAnimationTrigger){
        muteUnmute
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id) {
                            var status = doc.data["isMute"].toString()
                            onAnimationTrigger.onMicOnOff(mentorId,status)
                        }
                    }
                }
            }
    }
    fun deleteMuteUnmute(mentorId: String){
        muteUnmute.document(mentorId).delete()
    }
    interface OnNotificationTrigger{
        fun onNotificationForInvitePartner(channelName: String,fromUserId :String , fromUserName:String,fromUserImage:String)
        fun onNotificationForPartnerNotAccept(userName:String?,userImageUrl:String,fromUserId:String,declinedUserId: String)
        fun onNotificationForPartnerAccept(channelName:String?,timeStamp:String,isAccept: String,opponentMemberId: String,mentorId: String)
        fun onGetRoomId(currentUserRoomID:String?,mentorId: String)
        fun onShowAnim(mentorId: String,isCorrect:String,choiceAnswer:String,marks: String)
    }
    interface OnRandomUserTrigger{
        fun onSearchUserIdFetch(roomId:String)
    }
    interface OnAnimationTrigger{
        fun onOpponentShowAnim(opponentTeamId: String?,isCorrect: String,marks: String)
        fun onOpponentPartnerCut(teamId: String,isCorrect:String,choiceAnswer:String)
        fun onOpponentTeamCutCard(opponentTeamId: String,isCorrect:String,choiceAnswer:String)
        fun onMicOnOff(partnerUserId: String,status:String)
    }

    interface OnMakeFriendTrigger{
        fun onSentFriendRequest(fromMentorId:String,fromUserName:String,fromImageUrl:String,isAccept: String)
        fun onPlayAgainNotificationFromApi(userName:String, userImage:String)
        fun onPartnerPlayAgainNotification(userName: String,userImage: String,mentorId: String)
    }

    interface OnLiveStatus{
        fun onGetLiveStatus(status: String,mentorId: String)
    }
}