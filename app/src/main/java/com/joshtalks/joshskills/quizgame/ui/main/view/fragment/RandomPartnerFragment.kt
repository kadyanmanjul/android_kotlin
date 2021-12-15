package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentRandomPartnerBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchRandomRepo
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchRandomProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchRandomUserViewModel
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.MyBounceInterpolator
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val FOUR_USER_FOUND_MSG: String = "4 users found in Redis successfully"
const val NO_OPPONENT_FOUND = "No Opponent Team Found Please Retry"

class RandomPartnerFragment : Fragment(), FirebaseDatabase.OnRandomUserTrigger {

    lateinit var binding: FragmentRandomPartnerBinding
    var repository: SearchRandomRepo? = null
    var factory: SearchRandomProviderFactory? = null
    var searchRandomViewModel: SearchRandomUserViewModel? = null
    var currentUserId: String? = null
    var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    var userRoomId: String? = null

    var team1Id: String? = null
    var usersInTeam1: UsersInTeam1Random? = null
    var team2Id: String? = null
    var usersInTeam2: UsersInTeam2Random? = null

    var user1: User1Random? = null
    var user2: User2Random? = null
    var user3: User3Random? = null
    var user4: User4Random? = null

    var token: String? = null
    var channelName: String? = null
    var userId: String? = null

    private var team1UserId1: String? = null
    private var team1UserId2: String? = null
    private var team2UserId1: String? = null
    private var team2UserId2: String? = null

    private var team1User1Name: String? = null
    private var team1User2Name: String? = null
    private var team2User1Name: String? = null
    private var team2User2Name: String? = null

    private var team1User1Token: String? = null
    private var team1User2Token: String? = null
    private var team2User1Token: String? = null
    private var team2User2Token: String? = null

    private var team1User1ChannelName: String? = null
    private var team1User2ChannelName: String? = null
    private var team2User1ChannelName: String? = null
    private var team2User2ChannelName: String? = null

    private var team1User1ImageUrl: String? = null
    private var team1User2ImageUrl: String? = null
    private var team2User1ImageUrl: String? = null
    private var team2User2ImageUrl: String? = null

    private var opponentUserName: String? = null
    private var opponentUserImage: String? = null

    private var currentUserTeamId: String? = null

    private val PERMISSION_REQ_ID = 22
    private var REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var engine: RtcEngine? = null
    private var agora_app_id = "569a477f372a454b8101fc89ec6161e6"

    private var myUid = 0
    private var joined = false
    private var timer: CountDownTimer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        currentUserId = Mentor.getInstance().getUserId()
        setUpViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_random_partner,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            engine = RtcEngine.create(activity, agora_app_id, iRtcEngineEventHandler)
            //engine = P2pRtc().initEngine(requireActivity())
        } catch (ex: Exception) {
            Timber.d(ex)
            // showToast(ex.message?:"")
        }
        setUserActive()
        setCurrentUserData()
        onBackPress()
        searchRandomUser(currentUserId ?: "")
        startTimer()
        try {
            firebaseDatabase.getRandomUserId(currentUserId ?: "", this)
        } catch (ex: Exception) {

        }

        dipDown(binding.vs1)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RandomPartnerFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun setCurrentUserData() {
        binding.team1User1Name.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl = Mentor.getInstance().getUser()?.photo?.replace("\n", "")
        binding.team1UserImage1.setUserImageOrInitials(
            imageUrl,
            Mentor.getInstance().getUser()?.firstName ?: "",
            30,
            isRound = true
        )
        //ImageAdapter.imageUrl(binding.team1UserImage1,imageUrl)
    }

    fun searchRandomUser(mentorId: String) {
        try {
            searchRandomViewModel?.getSearchRandomUserData(mentorId)
            activity?.let {
                searchRandomViewModel?.searchRandomData?.observe(it, {
                    Log.d("response_random", "searchRandomUser: " + it.data + " " + it.message)
                    if (it.message == FOUR_USER_FOUND_MSG) {
                        createRandomUserRoom(it.data)
                    }
                })
            }
        } catch (ex: Exception) {

        }

        //yaha par ham agar 4 user id mil gai hai tu create room wali api call karuga
        //then room create hone ke bad ham us room id ke response me data milgea us se call connect or
        //data show karuga 2 team wise
    }

    private fun startTimer() {
        timer = object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                if (userRoomId != null) {
                    moveFragment()
                    timer?.cancel()
                } else {
                    showToast(NO_OPPONENT_FOUND)
                    searchRandomViewModel?.deleteUserRadiusData(DeleteUserData(currentUserId ?: ""))
                    activity?.let {
                        searchRandomViewModel?.deleteData?.observe(it, {
                            AudioManagerQuiz.audioRecording.stopPlaying()
                            openChoiceScreen()
                            engine?.leaveChannel()
                            timer?.cancel()
                        })
                    }
                }
            }
        }.start()
    }

    fun setUpViewModel() {
        repository = SearchRandomRepo()
        factory = activity?.application?.let { SearchRandomProviderFactory(it, repository!!) }
        searchRandomViewModel = factory?.let {
            ViewModelProvider(this, it).get(SearchRandomUserViewModel::class.java)
        }
       // searchRandomViewModel?.statusChange(currentUserId, ACTIVE)
    }

    fun setUserActive() {
       // activity?.let { searchRandomViewModel?.statusResponse?.observe(it, {}) }
    }

    fun createRandomUserRoom(listOfUsers: ArrayList<String>) {
        try {
            searchRandomViewModel?.createRoomRandom(RoomRandom(listOfUsers))
            activity?.let {
                searchRandomViewModel?.roomRandomData?.observe(it, {
                    Log.d("response_roomid", "searchRandomUser: " + it.roomId)
                })
            }
        } catch (ex: Exception) {

        }
    }

    fun initializeUsersTeamsData(teamsData: TeamsDataRandom?) {
        team1Id = teamsData?.team1Id
        team2Id = teamsData?.team2Id

        usersInTeam1 = teamsData?.usersInTeam1
        usersInTeam2 = teamsData?.usersInTeam2

        //Team 1 ke Users
        user1 = usersInTeam1?.user1
        user2 = usersInTeam1?.user2

        //Team 2 ke Users
        user3 = usersInTeam2?.user3
        user4 = usersInTeam2?.user4

        //get all users Ids
        team1UserId1 = user1?.userId
        team1UserId2 = user2?.userId
        team2UserId1 = user3?.userId
        team2UserId2 = user4?.userId

        //get all users channelName
        team1User1ChannelName = user1?.channelName
        team1User2ChannelName = user2?.channelName
        team2User1ChannelName = user3?.channelName
        team2User2ChannelName = user4?.channelName

        //get all users Token
        team1User1Token = user1?.token
        team1User2Token = user2?.token
        team2User1Token = user3?.token
        team2User2Token = user4?.token

        //get all users Name
        team1User1Name = user1?.userName
        team1User2Name = user2?.userName
        team2User1Name = user3?.userName
        team2User2Name = user4?.userName

        //get all users Images
        team1User1ImageUrl = user1?.imageUrl
        team1User2ImageUrl = user2?.imageUrl
        team2User1ImageUrl = user3?.imageUrl
        team2User2ImageUrl = user4?.imageUrl


        if (team1UserId1 == currentUserId || team1UserId2 == currentUserId) {
            //yaha par ham jo current user hai usko niche set karte hai or uske partner ko bhi

            currentUserTeamId = team1Id
            if (team1UserId1 == currentUserId) {
                opponentUserImage = team1User2ImageUrl
                opponentUserName = team1User2Name
            } else {
                opponentUserImage = team1User1ImageUrl
                opponentUserName = team1User1Name
            }

//            val imageUrl1 = team1User1ImageUrl?.replace("\n","")
//            ImageAdapter.imageUrl(binding.team1UserImage1,imageUrl1)
//            binding.team1User1Name.text = team1User1Name

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.team1UserImage2,imageUrl2)
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = team1User2Name

            callConnectUser1AndUser2(team1User1ChannelName)

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team2UserImage1,imageUrl3)
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team2User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = team2User1Name

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.team2UserImage2,imageUrl4)
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = team2User2Name

//            lifecycleScope.launch (Dispatchers.IO){
//                delay(4000)
//                moveFragment()
//            }

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            currentUserTeamId = team2Id
            if (team2UserId1 == currentUserId) {
                opponentUserImage = team2User2ImageUrl
                opponentUserName = team2User2Name
            } else {
                opponentUserImage = team2User1ImageUrl
                opponentUserName = team2User1Name
            }

//            val imageUrl1 = team2User1ImageUrl?.replace("\n","")
//            ImageAdapter.imageUrl(binding.team1UserImage1,imageUrl1)
//            binding.team1User1Name.text = team2User1Name

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team1UserImage2,imageUrl2)
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = team2User2Name

            callConnectUser1AndUser2(team2User1ChannelName)

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.team2UserImage1,imageUrl3)
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team1User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = team1User1Name

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.team2UserImage2,imageUrl4)
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = team1User2Name

//            lifecycleScope.launch (Dispatchers.IO){
//                delay(4000)
//                moveFragment()
//            }
        }

    }

    override fun onSearchUserIdFetch(roomId: String) {
        Log.d("random_roomid", "onSearchUserIdFetch: " + roomId)
        userRoomId = roomId
        try {
            searchRandomViewModel?.getRandomUserDataByRoom(
                RandomRoomData(
                    userRoomId ?: "",
                    currentUserId ?: ""
                )
            )
            activity?.let {
                searchRandomViewModel?.randomRoomUser?.observe(it, Observer {
                    Log.d("response_roomid", "searchRandomUser: " + it.roomId)
                    initializeUsersTeamsData(it?.teamData)
                    binding.image9.pauseAnimation()
                    timer?.cancel()
                    timer?.onFinish()
                })
            }
        } catch (ex: Exception) {
            //showToast(ex.message?:"")
            Log.d("error_resp", "getRandomUserDataByRoom: " + ex.message)
        }
    }

    fun callConnectUser1AndUser2(channelName: String?) {
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
            CoroutineScope(Dispatchers.IO).launch {
                joinChannel(channelName ?: "")
            }
        }
    }

    private fun joinChannel(channelId: String) {
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        var accessToken: String? =
            "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
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
        if (res != 0) {
            return
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

    fun moveFragment() {
        //val startTime :String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    RandomTeamMateFoundFragnment.newInstance(
                        userRoomId ?: "",
                        opponentUserImage ?: "",
                        opponentUserName ?: ""
                    ), "RandomPartnerFragment"
                )
                ?.commit()
        }, 4000)
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //CustomDialogQuiz(activity!!).show()
                    showDialog()
                }
            })
    }

    private fun showDialog() {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
            if (userRoomId != null) {
                searchRandomViewModel?.getClearRadius(
                    SaveCallDurationRoomData(
                        userRoomId ?: "",
                        currentUserId ?: "",
                        currentUserTeamId ?: "",
                        ""
                    )
                )
                activity?.let {
                    searchRandomViewModel?.clearRadius?.observe(it, Observer {
                        dialog.dismiss()
                        AudioManagerQuiz.audioRecording.stopPlaying()
                        openChoiceScreen()
                        engine?.leaveChannel()
                    })
                }
            } else {
                searchRandomViewModel?.deleteUserRadiusData(DeleteUserData(currentUserId ?: ""))
                activity?.let {
                    searchRandomViewModel?.deleteData?.observe(it, Observer {
                        dialog.dismiss()
                        AudioManagerQuiz.audioRecording.stopPlaying()
                        openChoiceScreen()
                        engine?.leaveChannel()
                    })
                }
            }
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragnment.newInstance(), "Question"
            )
            ?.remove(this)
            ?.commit()
    }

    //    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback{
//        override fun onChannelJoin() {
//            super.onChannelJoin()
//        }
//
//        override fun onConnect(callId: String) {
//            super.onConnect(callId)
//        }
//
//        override fun onDisconnect(callId: String?, channelName: String?) {
//            super.onDisconnect(callId, channelName)
//        }
//
//        override fun onSpeakerOff() {
//            super.onSpeakerOff()
//        }
//
//        override fun onNetworkLost() {
//            super.onNetworkLost()
//        }
//
//        override fun onPartnerLeave() {
//            super.onPartnerLeave()
//        }
//    }
    private val iRtcEngineEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onError(err: Int) {
            // showToast("Error")
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
            // showToast("Leave Channel")
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            myUid = uid
            joined = true
            // showToast("Success fully Join"+uid)
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            // showToast("Remote Audio State Change")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            // showToast("User Joined")
            //Here pass data of the current user
            // moveFragment(favouriteUserId)
        }

        override fun onUserOffline(uid: Int, reason: Int) {}

        override fun onActiveSpeaker(uid: Int) {
            super.onActiveSpeaker(uid)
        }
    }

    private fun dipDown(targetView: View) {
        val myAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_anim)
        val interpolator = MyBounceInterpolator(0.8, 18.0)
        myAnim.interpolator = interpolator
        myAnim.duration = 3000
        myAnim.repeatCount = Animation.INFINITE
        targetView.startAnimation(myAnim)
    }

}