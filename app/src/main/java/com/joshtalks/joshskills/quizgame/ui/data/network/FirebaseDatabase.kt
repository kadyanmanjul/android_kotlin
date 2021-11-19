package com.joshtalks.joshskills.quizgame.ui.data.network

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.google.firebase.firestore.DocumentSnapshot
import com.joshtalks.joshskills.core.showToast

class FirebaseDatabase {
//    private val firestore by lazy { Firebase.firestore }
//    private val channelCollection by lazy { firestore.collection("Request") }

    private var database = FirebaseFirestore.getInstance()
    private var collectionReference:CollectionReference = database.collection("Request")
    private var acceptRequestCollection:CollectionReference = database.collection("Accept_Request")

    private var collectionCurrentUserRoomId:CollectionReference = database.collection("UserRoom")
    private var requestDecline : CollectionReference = database.collection("request_decline")
    private var animShow:CollectionReference = database.collection("ShowAnim")

    private var opponentAnimShow:CollectionReference = database.collection("OpponentShowAnim")

    private var randomUser:CollectionReference = database.collection("RandomUser")

    private var partnerShowCutCard:CollectionReference = database.collection("PartnerShowCut")
    private var opponentShowCutCard:CollectionReference = database.collection("OpponentShowCut")



    //var mentorId : String = Mentor.getInstance().getUserId()
    var userName :String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl :String?= Mentor.getInstance().getUser()?.photo

    var onNotificationTrigger : OnNotificationTrigger?=null
    var onRandomUserTrigger : OnRandomUserTrigger?=null
    var onAnimationTrigger : OnAnimationTrigger?=null


    fun createRequest(favUserId:String?,channelName:String?,mentorId: String){
         val channel: HashMap<String,Any> = HashMap()
         channel["fromUserId"] = mentorId
         channel["fromUserName"] = userName?:""
         channel["fromImageUrl"] = imageUrl?: ""
         channel["channelName"] = channelName?:""
         channel["timestamp"] = FieldValue.serverTimestamp()
         channel["isAccept"] = "false"
         collectionReference.document(favUserId?:"").set(channel)
     }


    fun acceptRequest(opponentMemberId:String, isAccept:String, opponentMemberName: String , channelName: String,mentorId: String){
        val channel: HashMap<String,Any> = HashMap()
        channel["mentorId"] = mentorId
        channel["opponentMemberId"] = opponentMemberId
        channel["opponentMemberName"] = opponentMemberName
        channel["channelName"] = channelName
        channel["timestamp"] = FieldValue.serverTimestamp()
        channel["isAccept"] = isAccept
        acceptRequestCollection.document(opponentMemberId).set(channel)
    }

    fun getAcceptCall(mentorId:String){
        acceptRequestCollection
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            var channelName = doc.data["channelName"].toString()
                            var timestamp = doc.data["timestamp"].toString()
                            var isAccept = doc.data["isAccept"].toString()
                            var opponentMemberId = doc.data["opponentMemberId"].toString()
                            var mentorIdIdAcceptedUser = doc.data["mentorId"].toString()
                            if (isAccept == "true"){
                                onNotificationTrigger?.onNotificationForPartnerAccept(channelName,timestamp,isAccept,opponentMemberId,mentorIdIdAcceptedUser)
                            }
                        }
                    }
                }
            }
    }

//    fun delteData10Sec(){
//        acceptRequestCollection.endAt()
//    }

     fun getUserDataFromFirestore(mentorId: String,onNotificationTrigger: OnNotificationTrigger){
       this.onNotificationTrigger=onNotificationTrigger
       collectionReference
        .addSnapshotListener { value, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            for (doc in value!!) {
                if (doc.exists()) {
                    if (mentorId == doc.id){
                        var channelName:String = doc.data["channelName"].toString()
                        var fromUserId = doc.data["fromUserId"].toString()
                        var fromUserName = doc.data["fromUserName"].toString()
                        var fromImageUrl = doc.data["fromImageUrl"]?.toString()
                        onNotificationTrigger.onNotificationForInvitePartner(channelName,fromUserId,fromUserName,fromImageUrl?: "")
                    }
                }
            }
        }
     }

     fun deleteUserData(mentorId:String,fromUserMentorId:String){
       // collectionReference.document(documentId).delete()
        collectionReference
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            var fromUserId = doc.data["fromUserId"].toString()
                            if (fromUserId == fromUserMentorId){
                                Log.d("sagar", "deleteUserData: "+fromUserId)
                                collectionReference.document(mentorId).delete().addOnCompleteListener(
                                    OnCompleteListener {
                                        createRequestDecline(fromUserId,userName,imageUrl)
                                    })
                            }
                        }
                    }
                }
            }
    }

    fun createRequestDecline(fromUserId:String, declineUserName:String?,declineUserImage:String?){
        val channel: HashMap<String,Any> = HashMap()
        channel["declineUserName"] = declineUserName?:""
        channel["declineUserImage"] = declineUserImage?:""
        requestDecline.document(fromUserId).set(channel)
    }

    fun getDeclineCall(mentorId:String){
        requestDecline
            .addSnapshotListener { value, e ->
                if (e != null) {
                   // return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            val declinedUserName = doc.data["declineUserName"].toString()
                            val declinedUserImage = doc.data["declineUserImage"].toString()
                            onNotificationTrigger?.onNotificationForPartnerNotAccept(declinedUserName,declinedUserImage,mentorId)
                        }
                    }
                }
            }
    }

    fun deleteDeclineData(mentorId:String){
        //requestDecline.document(mentorId).delete()
        requestDecline
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                         requestDecline.document(mentorId).delete()
                        }
                    }
                }
            }
    }

    fun deleteDataAcceptRequest(mentorId:String){
        acceptRequestCollection.document(mentorId).delete()
        acceptRequestCollection
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            requestDecline.document(mentorId).delete()
                        }
                    }
                }
            }
    }

    fun getCurrentUserRoomId(mentorId:String,onNotificationTrigger: OnNotificationTrigger){
        this.onNotificationTrigger = onNotificationTrigger
        var currentUserRoomId:String?=null
        collectionCurrentUserRoomId
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            currentUserRoomId = doc.data["room_id"].toString()
                            onNotificationTrigger.onGetRoomId(currentUserRoomId,mentorId)
                        }
                    }
                }
            }
    }


    fun acceptOrDeclineRequest(mentorId:String, opponentMemberId: String, isAccept: String, ){

    }

    fun createShowAnimForAnotherUser(partnerId: String?,isCorrect:String,choiceAnswer:String,marks:String) {
        val channel: HashMap<String,Any> = HashMap()
        channel["isCorrect"] = isCorrect
        channel["choiceAnswer"] = choiceAnswer
        channel["marks"] = marks
        animShow.document(partnerId?:"").set(channel)
    }

    fun getAnimShow(mentorId:String,onNotificationTrigger: OnNotificationTrigger){
        this.onNotificationTrigger=onNotificationTrigger
        animShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            val isCorrect = doc.data["isCorrect"].toString()
                            val choiceAnswer=doc.data["choiceAnswer"].toString()
                            val marks = doc.data["marks"].toString()
                            onNotificationTrigger.onShowAnim(mentorId,isCorrect,choiceAnswer,marks)
                        }
                    }
                }
            }
    }

    fun getRandomUserId(mentorId:String,onRandomUserTrigger: OnRandomUserTrigger){
        this.onRandomUserTrigger = onRandomUserTrigger
        randomUser
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (mentorId == doc.id){
                            val roomId = doc.data["room_id"].toString()
                            onRandomUserTrigger.onSearchUserIdFetch(roomId)
                        }
                    }
                }
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
                        if (partnerId == doc.id){
                            animShow.document(partnerId).delete()
                        }
                    }
                }
            }
    }

    fun createShowAnimForOpponentTeam(opponentTeamId: String?, correct: String?,marks: String?) {
        val channel: HashMap<String,Any> = HashMap()
        channel["isCorrect"] = correct?:""
        channel["marks"] = marks?:""
        opponentAnimShow.document(opponentTeamId?:"").set(channel)
    }

    fun getOpponentShowAnim(teamId:String,onAnimationTrigger: OnAnimationTrigger){
        this.onAnimationTrigger=onAnimationTrigger
        opponentAnimShow
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id){
                            val isCorrect = doc.data["isCorrect"].toString()
                            val marks = doc.data["marks"].toString()
                            onAnimationTrigger.onOpponentShowAnim(teamId,isCorrect,marks)
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
                        if (teamId == doc.id){
                            opponentAnimShow.document(teamId).delete()
                        }
                    }
                }
            }
    }

    fun createPartnerShowCutCard(currentUserTeamId: String, correct: String, choiceAnswer: String) {
        val channel: HashMap<String,Any> = HashMap()
        channel["isCorrect"] = correct
        channel["choiceAnswer"] =choiceAnswer
        partnerShowCutCard.document(currentUserTeamId).set(channel)
    }

    fun getPartnerCutCard(teamId: String){
        partnerShowCutCard
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (teamId == doc.id){
                            val isCorrect = doc.data["isCorrect"].toString()
                            val choiceAnswer = doc.data["choiceAnswer"].toString()
                            onAnimationTrigger?.onOpponentPartnerCut(teamId,isCorrect,choiceAnswer)
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
                        if (teamId == doc.id){
                            partnerShowCutCard.document(teamId).delete()
                        }
                    }
                }
            }
    }

    fun createOpponentTeamShowCutCard(opponentTeamId: String, correct: String, choiceAnswer: String) {
        val channel: HashMap<String,Any> = HashMap()
        channel["isCorrect"] = correct
        channel["choiceAnswer"] = choiceAnswer
        opponentShowCutCard.document(opponentTeamId).set(channel)
    }

    fun getOpponentCutCard(opponentTeamId: String){
        opponentShowCutCard
            .addSnapshotListener { value, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (doc in value!!) {
                    if (doc.exists()) {
                        if (opponentTeamId == doc.id){
                            val isCorrect = doc.data["isCorrect"].toString()
                            val choiceAnswer = doc.data["choiceAnswer"].toString()
                            onAnimationTrigger?.onOpponentTeamCutCard(opponentTeamId,isCorrect,choiceAnswer)
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
                        if (teamId == doc.id){
                            opponentShowCutCard.document(teamId).delete()
                        }
                    }
                }
            }
    }

    interface OnNotificationTrigger{
        fun onNotificationForInvitePartner(channelName: String,fromUserId :String , fromUserName:String,fromUserImage:String)
        fun onNotificationForPartnerNotAccept(userName:String?,userImageUrl:String,fromUserId:String)
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
    }
}