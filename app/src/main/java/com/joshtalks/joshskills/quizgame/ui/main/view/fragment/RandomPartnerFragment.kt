package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LEAVE_THE_GAME
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentRandomPartnerBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchRandomProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchRandomUserViewModel
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.CustomDialogQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.quizgame.util.UtilsQuiz
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
const val USER_DELETED_SUCCESSFULLY = "User deleted successfully"

class RandomPartnerFragment : Fragment(), FirebaseDatabase.OnRandomUserTrigger {

    lateinit var binding: FragmentRandomPartnerBinding

    var factory: SearchRandomProviderFactory? = null
    var searchRandomViewModel: SearchRandomUserViewModel? = null
    var currentUserId = Mentor.getInstance().getId()
    var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    var firebasetemp: FirebaseTemp = FirebaseTemp()

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

    private var engine: RtcEngine? = null
    private var timer: CountDownTimer? = null

    // private var isUiActive: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        binding.container.setBackgroundColor(Color.WHITE)
        //  FirebaseDatabase().getRoomTime(currentUserId ?: "", this)

        try {
            engine = P2pRtc().getEngineObj()
            P2pRtc().addListener(callback)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
        setCurrentUserData()
        onBackPress()
        searchRandomUser(currentUserId)
        startTimer()
        try {
            firebaseDatabase.getRandomUserId(currentUserId, this)
        } catch (ex: Exception) {

        }
        UtilsQuiz.dipDown(binding.vs1, requireActivity())
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RandomPartnerFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun setUpViewModel() {
        factory = activity?.application?.let { SearchRandomProviderFactory(it) }
        searchRandomViewModel = factory?.let {
            ViewModelProvider(this, it).get(SearchRandomUserViewModel::class.java)
        }
        // searchRandomViewModel?.statusChange(currentUserId, ACTIVE)
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
    }

    fun searchRandomUser(mentorId: String) {
        try {
            searchRandomViewModel?.getSearchRandomUserData(mentorId)
            activity?.let {
                searchRandomViewModel?.searchRandomData?.observe(it, {
                    if (it.message == FOUR_USER_FOUND_MSG) {
                        createRandomUserRoom(it.data)
                    }
                })
            }
        } catch (ex: Exception) {

        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                if (userRoomId != null) {
                    moveFragment()
                    timer?.cancel()
                } else {
                    try {
                        Toast.makeText(context, NO_OPPONENT_FOUND, Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                    }
                    searchRandomViewModel?.deleteUserRadiusData(DeleteUserData(currentUserId))
                    activity?.let {
                        searchRandomViewModel?.deleteData?.observe(it, {
                            if (it.message == USER_DELETED_SUCCESSFULLY) {
                                firebasetemp.changeUserStatus(currentUserId, ACTIVE)
                                AudioManagerQuiz.audioRecording.stopPlaying()
                                openChoiceScreen()
                                engine?.leaveChannel()
                                timer?.cancel()
                            }
                        })
                    }
                }
            }
        }.start()
    }

    fun createRandomUserRoom(listOfUsers: ArrayList<String>) {
        try {
            searchRandomViewModel?.createRoomRandom(RoomRandom(listOfUsers, currentUserId))
            activity?.let {
                searchRandomViewModel?.roomRandomData?.observe(it, {
                    Timber.d(it.roomId)
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

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = UtilsQuiz.getSplitName(team1User2Name)

            callConnectUser1AndUser2(team1User1ChannelName)

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team2User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = UtilsQuiz.getSplitName(team2User1Name)

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = UtilsQuiz.getSplitName(team2User2Name)

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            currentUserTeamId = team2Id
            if (team2UserId1 == currentUserId) {
                opponentUserImage = team2User2ImageUrl
                opponentUserName = team2User2Name
            } else {
                opponentUserImage = team2User1ImageUrl
                opponentUserName = team2User1Name
            }

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = UtilsQuiz.getSplitName(team2User2Name)

            callConnectUser1AndUser2(team2User1ChannelName)

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team1User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = UtilsQuiz.getSplitName(team1User1Name)

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = UtilsQuiz.getSplitName(team1User2Name)

        }

    }

    override fun onSearchUserIdFetch(roomId: String) {
        userRoomId = roomId
        try {
            if (userRoomId != null) {
                // if (isUiActive){
                searchRandomViewModel?.getRandomUserDataByRoom(
                    RandomRoomData(
                        userRoomId ?: "",
                        currentUserId
                    )
                )
                activity?.let {
                    searchRandomViewModel?.randomRoomUser?.observe(it, Observer {
                        Timber.d(it.roomId)
                        initializeUsersTeamsData(it?.teamData)
                        binding.image9.pauseAnimation()
                        timer?.cancel()
                        timer?.onFinish()
                    })
                }
                // }
            }
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    fun callConnectUser1AndUser2(channelName: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            joinChannel(channelName ?: "")
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

    fun moveFragment() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    RandomTeamMateFoundFragment.newInstance(
                        userRoomId ?: "",
                        opponentUserImage ?: "",
                        opponentUserName ?: "",
                        currentUserTeamId ?: "",
                        0
                    ), "RandomPartnerFragment"
                )
                ?.commit()
        }, 4000)
    }

//    fun moveFragmentWhenRoomCreated(time: Long) {
//        val android = ((System.currentTimeMillis() / 1000) % 60)
//        val serverTime = (time) % 60
//
//        val countDown = serverTime - android
//
//        val handler = Handler(Looper.getMainLooper())
//        handler.postDelayed({
//            val fm = activity?.supportFragmentManager
//            fm?.beginTransaction()
//                ?.replace(
//                    R.id.container,
//                    RandomTeamMateFoundFragment.newInstance(
//                        userRoomId ?: "",
//                        opponentUserImage ?: "",
//                        opponentUserName ?: "",
//                        currentUserTeamId ?: ""
//                    ), "RandomPartnerFragment"
//                )
//                ?.commit()
//        }, (countDown - 15) * 1000)
//    }


    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomDialogQuiz(requireActivity()).showDialog(::positiveBtnAction)
                }
            })
    }

    fun positiveBtnAction() {
        if (userRoomId != null) {
            searchRandomViewModel?.getClearRadius(
                SaveCallDurationRoomData(
                    userRoomId ?: "",
                    currentUserId,
                    currentUserTeamId ?: "",
                    ""
                )
            )
            activity?.let {
                searchRandomViewModel?.clearRadius?.observe(it, Observer {
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openChoiceScreen()
                    engine?.leaveChannel()
                })
            }
        } else {
            searchRandomViewModel?.deleteUserRadiusData(DeleteUserData(currentUserId))
            activity?.let {
                searchRandomViewModel?.deleteData?.observe(it, Observer {
                    firebasetemp.changeUserStatus(currentUserId, ACTIVE)
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openChoiceScreen()
                    engine?.leaveChannel()
                })
            }
        }
    }

    fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "Question"
            )
            ?.remove(this)
            ?.commit()
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback {
        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    PrefManager.put(USER_LEAVE_THE_GAME, true)
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }
}