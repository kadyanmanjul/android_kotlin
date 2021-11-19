package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentFavouritePracticeBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToTokenResponse
import com.joshtalks.joshskills.quizgame.ui.data.model.ChannelData
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.FavouriteAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.FavouriteViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ViewModelProviderFactory
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.android.synthetic.main.fragment_favourite_practice.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class FavouritePartnerFragment : Fragment(),FavouriteAdapter.QuizBaseInterface,FirebaseDatabase.OnNotificationTrigger {

    private lateinit var binding:FragmentFavouritePracticeBinding
    private var favouriteAdapter: FavouriteAdapter? = null
    private var favouriteViewModel: FavouriteViewModel? = null
    private var firebaseDatabase:FirebaseDatabase = FirebaseDatabase()
    private var channelName:String?=null
    private var fromTokenId:String?=null
    private var fromUserId:String?=null
    private var favouriteUserId:String?=null
    private val PERMISSION_REQ_ID = 22
    private var agora_app_id="569a477f372a454b8101fc89ec6161e6"
    private var engine: RtcEngine? = null
    private var myUid = 0
    private var joined = false
    private var activityInstance: FragmentActivity?=null
    private var repository : FavouriteRepo?=null
    private var factory : ViewModelProviderFactory?=null
    private var mentorId:String?=null

  //  private var requestDecline : CollectionReference = database.collection("request_decline")

    private var REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
     )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mentorId = Mentor.getInstance().getUserId()
        setupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_favourite_practice,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this
        favouriteAdapter= activity?.applicationContext?.let { FavouriteAdapter(it, ArrayList(),this) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityInstance = activity

        Log.d("user_id", "onViewCreated: "+Mentor.getInstance().getUserId())

        // It's is use for initialize rtc engine
//        try {
//            engine = RtcEngine.create(activity, agora_app_id, iRtcEngineEventHandler)
//        }catch (ex:Exception){
//          Timber.d(ex)
//        }

        try {
            engine = P2pRtc().initEngine(requireActivity())
        }catch (ex:Exception){
            Timber.d(ex)
        }

        try {
            //This is use for get favourite practice partner list
            getFavouritePracticePartner()
            //It's is use for get current user channel data further use for join in the agora call
            getFromAgoraToken()
            //This is use for change user status
            changeStatus()
        }catch (ex:Exception){
            Timber.d(ex)
        }
       // addToTeam()

        //This is use for when another use want to coonect with current user so they will create entry for
        //own channel id and we will subscribe this id if any changes then create notification
        try {
            firebaseDatabase.getUserDataFromFirestore(mentorId?:"",this)//yaha par mentor id pass karna hai
        }catch (ex:Exception){
            Timber.d(ex)
        }

        //it's use for delete notification data when user decline call
        if (isAdded && activityInstance != null) {
            deleteData()
        }else{
            Log.d("crash_error", "onViewCreated: ")
        }

        if (isAdded && activityInstance!=null){
            getAcceptCall()
        }
        else{
            Log.d("crash_error", "onViewCreated: ")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FavouritePartnerFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
    private fun setupViewModel() {
        repository = FavouriteRepo()
        factory = activity?.application?.let { ViewModelProviderFactory(it, repository!!) }
        favouriteViewModel = factory?.let {
            ViewModelProvider(this, it).get(FavouriteViewModel::class.java)
        }
        favouriteViewModel?.fetchFav(mentorId?:"")
        favouriteViewModel?.statusChange(mentorId,"active")
    }
    private fun initRV( favouriteList: ArrayList<Favourite>?) {
        favouriteAdapter?.addItems(favouriteList)
        binding.recycleView.setHasFixedSize(true)
        binding.recycleView.layoutManager = LinearLayoutManager(activity)
        favouriteAdapter= activity?.let { FavouriteAdapter(it,favouriteList,this) }
        recycle_view.adapter = favouriteAdapter
    }
    private fun fromAgoraToken(channelData: ChannelData?){
        channelName = channelData?.channelName
        fromTokenId = channelData?.token
        fromUserId = channelData?.userUid
    }
     fun onBack(){

    }

    override fun onClickForGetToken(favourite: Favourite?) {
        favouriteUserId = favourite?.uuid
        favouriteUserId.let { firebaseDatabase.createRequest(favouriteUserId,channelName,mentorId!!) }
    }

    fun initializeAgoraCall(channelName:String){
        // Check permission
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
            CoroutineScope(Dispatchers.IO).launch {
                joinChannel(channelName)
         }
            //WebRtcEngine.initLibrary()
        }
    }
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (activity?.let { ContextCompat.checkSelfPermission(it, permission) } !=
            PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    REQUESTED_PERMISSIONS,
                    requestCode
                )
            }
            return false
        }
        return true
    }

    private fun joinChannel(channelId: String) {
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        var accessToken: String? = "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(
                accessToken,
                "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
            )
        ) {
            accessToken = null
        }
        engine?.enableAudioVolumeIndication(1000, 3, true)
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res = engine?.joinChannel(accessToken, channelId, "Extra Optional Data", 0, option)
        if (res != 0) { return }
    }

//    private val iRtcEngineEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
//         override fun onError(err: Int) {
//            //showToast("Error")
//        }
//
//        override fun onLeaveChannel(stats: RtcStats) {
//            super.onLeaveChannel(stats)
//           // showToast("Leave Channel")
//        }
//        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
//            myUid = uid
//            joined = true
//           // showToast("Success fully Join"+uid)
//        }
//
//       override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
//            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
//            //showToast("Remote Audio State Change")
//        }
//
//        override fun onUserJoined(uid: Int, elapsed: Int) {
//            super.onUserJoined(uid, elapsed)
//           // showToast("User Joined")
//            //Here pass data of the current user
//           // moveFragment(favouriteUserId)
//        }
//
//        override fun onUserOffline(uid: Int, reason: Int) {
//           // showToast("User Offline")
//        }
//
//        override fun onActiveSpeaker(uid: Int) {
//            super.onActiveSpeaker(uid)
//           // showToast("Active Speaker")
//        }
//    }

    override fun onNotificationForInvitePartner(channelName: String, fromUserId: String, fromUserName: String, fromUserImageUrl:String) {
         var i=0
        try {
            binding.notificationCard.visibility = View.VISIBLE
            binding.progress.animateProgress()
            binding.userName.text =fromUserName
            val imageUrl=fromUserImageUrl.replace("\n","")

            activity?.let {
                Glide.with(it)
                    .load(imageUrl)
                    .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
                    .into(binding.userImage)
            }
        }catch (ex:Exception){
            Timber.d(ex)
        }

         binding.buttonAccept.setOnClickListener(View.OnClickListener {
             i=1
             binding.notificationCard.visibility = View.INVISIBLE

             // initializeAgoraCall(channelName)
             // accept request me ham jis se hame request send ki hai uski id ke base par ham ek entry create
             //karege firestore me or is_accept = true/false rakhage
             //fromUserId = jis se hame request send ki hai

             //jab move karege tu team id bhi sath leke jana hai
             //channel name = team id yaha  response?.channelName = team id ye sath leke
             //jana hai kyoki room ka data lane ke liye use ayegi

             // user id me opponent ka id ayegi mtlb favourite partner ki id ayegi
             favouriteViewModel?.getChannelData(mentorId,channelName)
             activity?.let {
                 favouriteViewModel?.agoraToToken?.observe(it, Observer {
                     if (it?.message.equals("Team created successfully")){
                         firebaseDatabase.acceptRequest(fromUserId,"true",fromUserName,channelName,mentorId!!)
                         initializeAgoraCall(channelName)
                         moveFragment(fromUserId,channelName)
                     }else{
                         binding.notificationCardAlready.visibility = View.VISIBLE
                     }
                 })
             }

             binding.alreadyNotification.setOnClickListener(View.OnClickListener {
                 binding.notificationCardAlready.visibility = View.INVISIBLE
             })
         })


        //yaha abhi testin ke liye hamea yahi 5186d216-3a30-46e4-8fcb-126021fb14d5 id rahegi kyoki
        //create reqest me 86 wali hai
        binding.butonDecline.setOnClickListener(View.OnClickListener {
            binding.notificationCard.visibility = View.INVISIBLE
            mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
        })


        //we have to do 10 second decline request done from firestore side beacuse is user not in fav screen so
        // request will no delete
        if (i!=1){
            lifecycleScope.launch {
                delay(15000L)
                binding.notificationCard.visibility = View.INVISIBLE
                mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
            }
        }

    }

    fun moveFragment(userId:String?,channelName: String?){
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(R.id.container,
                    TeamMateFoundFragnment.newInstance(userId?:"",channelName?:""),"TeamMateFoundFragnment")
                ?.addToBackStack(null)
                ?.commit()
    }

    // jab redmi y1 se login karege tu hame  5186d216-3a30-46e4-8fcb-126021fb14d5
    // ye wala mentor id pas karna hai
    //ye is current user ki mentor id hogi
    fun deleteData(){
       firebaseDatabase.getDeclineCall(mentorId?:"")
    }

    fun getAcceptCall(){
        firebaseDatabase.getAcceptCall(mentorId?:"")
    }

//    fun getDeclineCall(mentorId:String){
//        requestDecline
//            .addSnapshotListener { value, e ->
//                if (e != null) {
//                    // return@addSnapshotListener
//                }
//                for (doc in value!!) {
//                    if (doc.exists()) {
//                        if (mentorId == doc.id){
//                            var declinedUserName = doc.data["declineUserName"].toString()
//                            var declinedUserImage = doc.data["declineUserImage"].toString()
//                            onNotificationForPartnerNotAccept(declinedUserName,declinedUserImage,mentorId)
//                        }
//                    }
//                }
//            }
//    }


    override fun onNotificationForPartnerNotAccept(userName: String?, userImageUrl:String, fromUserId:String) {
        // jab redmi y1 se login karege tu hame  5186d216-3a30-46e4-8fcb-126021fb14d5
        // ye wala mentor id pas karna hai
        if (fromUserId == mentorId){
            try {
                val image=userImageUrl.replace("\n","")
                binding.notificationCardNotPlay.visibility = View.VISIBLE
                binding.userNameForNotPlay.text = userName
                activity?.let {
                    Glide.with(it)
                        .load(image)
                        .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
                        .into(binding.userImageForNotPaly)
                }
            }catch (ex:Exception){
                Timber.d(ex)
            }
        }

        binding.cancelNotification.setOnClickListener(View.OnClickListener {
            firebaseDatabase.deleteDeclineData(mentorId?:"")
            binding.notificationCardNotPlay.visibility = View.INVISIBLE
        })
    }

    override fun onNotificationForPartnerAccept(
        channelName: String?,
        timeStamp: String,
        isAccept: String,
        opponentMemberId:String,
        mentorIdIdAcceptedUser:String
    ) {
        if(isAccept=="true"){
            channelName?.let { initializeAgoraCall(it) }
            moveFragment(mentorIdIdAcceptedUser,channelName)
            firebaseDatabase.deleteDataAcceptRequest(opponentMemberId)
        }
    }

    override fun onGetRoomId(currentUserRoomID: String?,mentorIdIdAcceptedUser: String) {

    }

    override fun onShowAnim(mentorId:String,isCorrect: String,c:String,m:String) {

    }

    private fun getFavouritePracticePartner() {
        activity?.let {
            try {
                favouriteViewModel?.favData?.observe(it, Observer {
                    initRV(it.data)
                })
            }catch (ex:Exception){
                Timber.d(ex)
            }
        }
    }
    private fun getFromAgoraToken() {
        activity?.let {
            try {
                favouriteViewModel?.fromTokenData?.observe(it, Observer {
                    fromAgoraToken(it)
                })
            }catch (ex:Exception){
                Timber.d(ex)
            }
        }
    }
    private fun changeStatus(){
        activity?.let {
            try {
                favouriteViewModel?.statusResponse?.observe(it, Observer {
                })
            }catch (ex:Exception){
                Timber.d(ex)
            }
        }
    }
    private fun addToTeam(){
        var response : AgoraToTokenResponse?=null
        //favouriteViewModel?.getChannelData("717de699-1cf4-4e48-893a-05b5f0310ec7","d9928420-00e0-4cb2-ae70-ea93f1e411a9")
        activity?.let {
            favouriteViewModel?.agoraToToken?.observe(it, Observer {
                Log.d("addtoteam", "addToTeam: "+it)
                response = it
            })
        }
    }
}