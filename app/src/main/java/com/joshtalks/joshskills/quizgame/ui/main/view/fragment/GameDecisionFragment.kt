package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentWinScreenBinding
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameFirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.DecisionDataViewModelGame
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.DecisionDataViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.fragment_question.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class GameDecisionFragment : Fragment(), GameFirebaseDatabase.OnMakeFriendTrigger,
    P2pRtc.WebRtcEngineCallback {
    private lateinit var binding: FragmentWinScreenBinding
    private var marks: String? = null
    private var roomId: String? = null
    private var teamId: String? = null
    private var winnerTeamStatus: Boolean? = false

    private var factory: DecisionDataViewProviderFactory? = null
    private var decisionDataViewModel: DecisionDataViewModelGame? = null
    private var currentUserId: String = Mentor.getInstance().getId()
    private var opponentTeamMarks: String? = null
    private var isUiActive = false

    private var team1Id: String? = null
    private var usersInTeam1: UsersInTeam1? = null
    private var team2Id: String? = null
    private var usersInTeam2: UsersInTeam2? = null

    private var user1: User1? = null
    private var user2: User2? = null
    private var user3: User3? = null
    private var user4: User4? = null

    private var team1UserId1: String? = null
    private var team1UserId2: String? = null
    private var team2UserId1: String? = null
    private var team2UserId2: String? = null

    private var team1User1Name: String? = null
    private var team1User2Name: String? = null
    private var team2User1Name: String? = null
    private var team2User2Name: String? = null

    private var team1User1ImageUrl: String? = null
    private var team1User2ImageUrl: String? = null
    private var team2User1ImageUrl: String? = null
    private var team2User2ImageUrl: String? = null

    private var currentUserTeamId: String? = null
    private var opponentTeamId: String? = null
    private var callTimeCount: String? = null
    private var fromType: String? = null
    private var engine: RtcEngine? = null
    private var gameFirebaseDatabase: GameFirebaseDatabase = GameFirebaseDatabase()

    private var partnerId: String? = null
    private var partnerName: String? = null
    private var partnerImage: String? = null
    private var currentUserName: String = Mentor.getInstance().getUser()?.firstName ?: EMPTY
    private var currentUserImage: String = Mentor.getInstance().getUser()?.photo ?: EMPTY
    var time: Int? = 0

    val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            marks = it.getString(TEAM_SCORE)
            opponentTeamMarks = it.getString(OPPONENT_TEAM_MARKS)
            roomId = it.getString(ROOM_ID)
            teamId = it.getString(TEAM_ID)
            callTimeCount = it.getString(CALL_TIME)
            fromType = it.getString(FROM_TYPE)
        }
        setRoomUsersData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_win_screen,
                container,
                false
            )
        binding.vm = decisionDataViewModel
        binding.lifecycleOwner = this
        binding.clickHandler = this
        binding.executePendingBindings()
        binding.callTime.start()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.container.setBackgroundColor(Color.WHITE)

        if (PrefManager.getBoolValue(USER_LEAVE_THE_GAME)) {
            binding.team1User2Name.alpha = 0.5f
            binding.team1UserImage2Shadow.visibility = View.VISIBLE
        }
        showRoomUserData()
        GameAnalytics.push(GameAnalytics.Event.GAME_COMPLETED)

        try {
            engine = activity?.let { P2pRtc().initEngine(it) }
            P2pRtc().addListener(callback)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.currentUserMarks.text = marks
        binding.opponentTeamMarks.text = opponentTeamMarks

        onBackPress()

        when {
            marks?.toInt() ?: 0 > opponentTeamMarks?.toInt() ?: 0 -> {
                winnerTeamStatus = true
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(5)).toString()
                binding.opponentUserTotalMarks.text =
                    (opponentTeamMarks?.toInt()?.plus(0)).toString()
                binding.winningPointCurrent.text = FIVE
                binding.winningPointOpponent.text = ZERO
                binding.txtOpponentWin.visibility = View.VISIBLE
                binding.txtOpponentWin.text = YOU_WON
                binding.conglutions.visibility = View.VISIBLE
                binding.winImg11.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_congratulations)
            }
            marks?.toInt() ?: 0 == opponentTeamMarks?.toInt() ?: 0 -> {
                winnerTeamStatus = false
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(0)).toString()
                binding.opponentUserTotalMarks.text =
                    (opponentTeamMarks?.toInt()?.plus(0)).toString()
                binding.winningPointCurrent.text = ZERO
                binding.winningPointOpponent.text = ZERO
                binding.conglutions.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_matcg_draw)
            }
            else -> {
                winnerTeamStatus = false
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(0)).toString()
                binding.opponentUserTotalMarks.text =
                    (opponentTeamMarks?.toInt()?.plus(5)).toString()
                binding.winningPointCurrent.text = ZERO
                binding.winningPointOpponent.text = FIVE
                binding.txtWinner.text = WINNER
                binding.txtOpponentWin.visibility = View.VISIBLE
                binding.txtOpponentWin.text = OPPONENT_WON
                binding.conglutions.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_opponentwon)
                binding.winImg.visibility = View.VISIBLE
                binding.txtWinner.visibility = View.VISIBLE
                binding.conglutions.visibility = View.VISIBLE
            }
        }
        time = callTimeCount?.toInt()?.div(1000)

        getFriendRequest()
        activity?.let {
            decisionDataViewModel?.saveRoomDetailsData?.observe(it, Observer {
                val points = it.points

                lifecycleScope.launch(Dispatchers.Main) {
                    UtilsQuiz.showSnackBar(
                        binding.container,
                        Snackbar.LENGTH_SHORT,
                        "You earned +$points in this game"
                    )
                }
                getPlayAgainDataFromFirebase()
            })
        }

        binding.btnMakeNewTeam.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_MAKE_NEW_TEAM_BUTTON)
            AudioManagerQuiz.audioRecording.tickPlaying(requireActivity())
            makeNewTeam()
        }

        binding.btnAddPeople.setOnClickListener {
            binding.btnAddPeople.isEnabled = false
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_ADD_TO_FRIEND_LIST)
            AudioManagerQuiz.audioRecording.tickPlaying(requireActivity())
            gameFirebaseDatabase.createFriendRequest(
                currentUserId,
                currentUserName,
                currentUserImage,
                partnerId ?: EMPTY
            )
            lifecycleScope.launch(Dispatchers.Main) {
                if (isUiActive) {
                    delay(10000)
                    binding.btnAddPeople.isEnabled = true
                }
            }
        }

        binding.btnPlayAgain.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_PLAY_AGAIN_BUTTON)
            AudioManagerQuiz.audioRecording.tickPlaying(requireActivity())
            binding.btnPlayAgain.isEnabled = false
            gameFirebaseDatabase.createPlayAgainNotification(
                partnerId ?: EMPTY,
                currentUserName,
                currentUserImage
            )
            playAgainApiCall(PlayAgain(currentUserTeamId ?: EMPTY, currentUserId))
            lifecycleScope.launch(Dispatchers.Main) {
                if (isUiActive) {
                    delay(10000)
                    binding.btnPlayAgain.isEnabled = true
                }
            }
        }

        gameFirebaseDatabase.getPartnerPlayAgainNotification(currentUserId, this)
    }

    fun getFriendRequest() {
        gameFirebaseDatabase.getAcceptFriendRequest(currentUserId, this)
        gameFirebaseDatabase.getFriendRequests(currentUserId, this)
    }

    override fun onStart() {
        super.onStart()
        binding.callTime.base = SystemClock.elapsedRealtime().minus(callTimeCount?.toLong() ?: 0)
        binding.callTime.start()
    }

    override fun onResume() {
        super.onResume()
        isUiActive = true
    }

    companion object {
        @JvmStatic
        fun newInstance(
            marks: String,
            opponentTeamMarks: String,
            roomId: String?,
            teamId: String?,
            callTime: String?,
            fromType: String
        ) =
            GameDecisionFragment().apply {
                arguments = Bundle().apply {
                    putString(TEAM_SCORE, marks)
                    putString(OPPONENT_TEAM_MARKS, opponentTeamMarks)
                    putString(ROOM_ID, roomId)
                    putString(TEAM_ID, teamId)
                    putString(CALL_TIME, callTime)
                    putString(FROM_TYPE, fromType)
                }
            }
    }

    fun setUpViewModel(saveRoomDetails: SaveRoomDetails) {
        decisionDataViewModel?.saveRoomDetails(saveRoomDetails)
    }

    fun setRoomUsersData() {
        factory = DecisionDataViewProviderFactory(requireActivity().application)
        decisionDataViewModel =
            ViewModelProvider(this, factory!!).get(DecisionDataViewModelGame::class.java)
        decisionDataViewModel?.getRoomUserDataTemp(
            RandomRoomData(
                roomId ?: EMPTY,
                currentUserId
            )
        )
        decisionDataViewModel?.checkUserAlreadyFppOrNot(
            CheckAlreadyFpp(
                currentUserId,
                teamId ?: EMPTY
            )
        )
    }

    fun showRoomUserData() {
        activity?.let {
            decisionDataViewModel?.checkAlreadyFppLiveData?.observe(it, Observer {
                if (it.alreadyFpp == "false") {
                    binding.btnAddPeople.visibility = View.VISIBLE
                } else {
                    binding.btnAddPeople.visibility = View.INVISIBLE
                }
            })
        }
        activity?.let {
            decisionDataViewModel?.roomUserDataTemp?.observe(it, Observer {
                initializeUsersTeamsData(it.teamData)
                setUpViewModel(
                    SaveRoomDetails(
                        roomId,
                        teamId,
                        marks,
                        winnerTeamStatus,
                        time?.toString(),
                        currentUserId
                    )
                )
                deleteAllFromFirestore(currentUserId)
            })
        }
    }

    fun initializeUsersTeamsData(teamsData: TeamsData?) {
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

            if (team1UserId1 == currentUserId) {
                partnerId = team1UserId2
                partnerImage = team1User2ImageUrl
                partnerName = team1User2Name
            } else {
                partnerId = team1UserId1
                partnerImage = team1User1ImageUrl
                partnerName = team1User1Name
            }
            currentUserTeamId = team1Id
            opponentTeamId = team2Id


            binding.txtOnButtonPlayagain.text = "Play Again\nwith ${getSplitName(partnerName)}"

            if (currentUserId == team1UserId1) {
                val imageUrl1 = team1User1ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage1.setUserImageOrInitials(
                    imageUrl1,
                    team1User1Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User1Name.text = getSplitName(team1User1Name)

                val imageUrl2 = team1User2ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage2.setUserImageOrInitials(
                    imageUrl2,
                    team1User2Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User2Name.text = getSplitName(team1User2Name)
            } else {
                val imageUrl2 = team1User1ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage2.setUserImageOrInitials(
                    imageUrl2,
                    team1User2Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User2Name.text = getSplitName(team1User1Name)

                val imageUrl1 = team1User2ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage1.setUserImageOrInitials(
                    imageUrl1,
                    team1User2Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User1Name.text = getSplitName(team1User2Name)
            }
            val imageUrl3 = team2User1ImageUrl?.replace("\n", EMPTY)
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team2User1Name ?: EMPTY,
                30,
                true
            )
            binding.team2User1Name.text = getSplitName(team2User1Name)

            val imageUrl4 = team2User2ImageUrl?.replace("\n", EMPTY)
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team2User2Name ?: EMPTY,
                30,
                true
            )
            binding.team2User2Name.text = getSplitName(team2User2Name)

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {
            if (team2UserId1 == currentUserId) {
                partnerId = team2UserId2
                partnerImage = team2User2ImageUrl
                partnerName = team2User2Name
            } else {
                partnerId = team2UserId1
                partnerImage = team2User1ImageUrl
                partnerName = team2User1Name
            }

            binding.txtOnButtonPlayagain.text = "Play Again\nwith ${getSplitName(partnerName)}"

            currentUserTeamId = team2Id
            opponentTeamId = team1Id


            if (team2UserId1 == currentUserId) {
                val imageUrl1 = team2User1ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage1.setUserImageOrInitials(
                    imageUrl1,
                    team2User1Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User1Name.text = getSplitName(team2User1Name)

                val imageUrl2 = team2User2ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage2.setUserImageOrInitials(
                    imageUrl2,
                    team2User2Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User2Name.text = getSplitName(team2User2Name)
            } else {
                val imageUrl1 = team2User2ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage1.setUserImageOrInitials(
                    imageUrl1,
                    team2User2Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User1Name.text = getSplitName(team2User2Name)

                val imageUrl2 = team2User1ImageUrl?.replace("\n", EMPTY)
                binding.team1UserImage2.setUserImageOrInitials(
                    imageUrl2,
                    team2User1Name ?: EMPTY,
                    30,
                    true
                )
                binding.team1User2Name.text = getSplitName(team2User1Name)
            }

            val imageUrl3 = team1User1ImageUrl?.replace("\n", EMPTY)
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team1User1Name ?: EMPTY,
                30,
                true
            )
            binding.team2User1Name.text = getSplitName(team1User1Name)

            val imageUrl4 = team1User2ImageUrl?.replace("\n", EMPTY)
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team1User2Name ?: EMPTY,
                30,
                true
            )
            binding.team2User2Name.text = getSplitName(team1User2Name)
        }
    }

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
        val startTime: String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        decisionDataViewModel?.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: EMPTY,
                startTime.toInt().div(1000).toString(),
                currentUserId
            )
        )
        if (fromType == RANDOM) {
            decisionDataViewModel?.getClearRadius(
                SaveCallDurationRoomData(
                    roomId ?: EMPTY,
                    currentUserId,
                    currentUserTeamId ?: EMPTY,
                    callTimeCount ?: EMPTY
                )
            )
            activity?.let {
                decisionDataViewModel?.clearRadius?.observe(it, {
                    if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_AND_RADIUS) {
                        activity?.let {
                            decisionDataViewModel?.saveCallDuration?.observe(it, {
                                if (it.message == CALL_DURATION_RESPONSE) {
                                    val points = it.points
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        UtilsQuiz.showSnackBar(
                                            binding.container,
                                            Snackbar.LENGTH_SHORT,
                                            "You earned +$points for speaking in English"
                                        )
                                    }
                                    AudioManagerQuiz.audioRecording.stopPlaying()
                                    engine?.leaveChannel()
                                    binding.callTime.stop()
                                    openFavouritePartnerScreen()
                                }
                            })
                        }
                    }
                })
            }
        } else {
            decisionDataViewModel?.deleteUserRoomData(
                SaveCallDurationRoomData(
                    roomId ?: EMPTY,
                    currentUserId,
                    currentUserTeamId ?: EMPTY,
                    callTimeCount ?: EMPTY
                )
            )
            activity?.let {
                decisionDataViewModel?.deleteData?.observe(it, {
                    if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_FPP) {
                        activity?.let {
                            decisionDataViewModel?.saveCallDuration?.observe(it, {
                                if (it.message == CALL_DURATION_RESPONSE) {
                                    val points = it.points
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        UtilsQuiz.showSnackBar(
                                            binding.container,
                                            Snackbar.LENGTH_SHORT,
                                            "You earned +$points for speaking in English"
                                        )
                                    }
                                    AudioManagerQuiz.audioRecording.stopPlaying()
                                    engine?.leaveChannel()
                                    binding.callTime.stop()
                                    openFavouritePartnerScreen()
                                }
                            })
                        }
                    }
                })
            }
        }
    }

    fun openFavouritePartnerScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "Win"
            )
            ?.remove(this)
            ?.commit()
    }

    fun makeNewTeam() {
        val startTime: String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        decisionDataViewModel?.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: EMPTY,
                startTime.toInt().div(1000).toString(),
                currentUserId
            )
        )
        if (fromType == RANDOM) {
            decisionDataViewModel?.getClearRadius(
                SaveCallDurationRoomData(
                    roomId ?: EMPTY,
                    currentUserId,
                    currentUserTeamId ?: EMPTY,
                    callTimeCount ?: EMPTY
                )
            )
            activity?.let {
                decisionDataViewModel?.clearRadius?.observe(it, {
                    if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_AND_RADIUS) {
                        activity?.let {
                            decisionDataViewModel?.saveCallDuration?.observe(it, {
                                if (it.message == CALL_DURATION_RESPONSE) {
                                    val points = it.points
                                    if (points.toInt() >= 1) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            UtilsQuiz.showSnackBar(
                                                binding.container,
                                                Snackbar.LENGTH_SHORT,
                                                "You earned +$points for speaking in English"
                                            )
                                        }
                                    }
                                    AudioManagerQuiz.audioRecording.stopPlaying()
                                    engine?.leaveChannel()
                                    binding.callTime.stop()
                                    openFavouritePartnerScreen()
                                }
                            })
                        }
                    }
                })
            }
        } else {
            decisionDataViewModel?.deleteUserRoomData(
                SaveCallDurationRoomData(
                    roomId ?: EMPTY,
                    currentUserId,
                    currentUserTeamId ?: EMPTY,
                    callTimeCount ?: EMPTY
                )
            )
            activity?.let {
                decisionDataViewModel?.deleteData?.observe(it, {
                    if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_FPP) {
                        activity?.let {
                            decisionDataViewModel?.saveCallDuration?.observe(it, {
                                if (it.message == CALL_DURATION_RESPONSE) {
                                    val points = it.points
                                    if (points.toInt() >= 1) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            UtilsQuiz.showSnackBar(
                                                binding.container,
                                                Snackbar.LENGTH_SHORT,
                                                "You earned +$points for speaking in English"
                                            )
                                        }
                                    }
                                    AudioManagerQuiz.audioRecording.stopPlaying()
                                    engine?.leaveChannel()
                                    binding.callTime.stop()
                                    openFavouritePartnerScreen()
                                }
                            })
                        }
                    }
                })
            }
        }
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback {
        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                PrefManager.put(USER_LEAVE_THE_GAME, false)
                binding.btnPlayAgain.isEnabled = false
                requireActivity().runOnUiThread {
                    engine?.leaveChannel()
                    binding.callTime.stop()
                    binding.team1User2Name.alpha = 0.5f
                    binding.team1UserImage2Shadow.visibility = View.VISIBLE
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }

    override fun onSentFriendRequest(
        fromMentorId: String,
        fromUserName: String,
        fromImageUrl: String,
        isAccept: String
    ) {

        try {
            binding.btnAddPeople.visibility = View.INVISIBLE
            if (isUiActive)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCard)
            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromImageUrl.replace("\n", EMPTY)
            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.eee.setOnClickListener {
            binding.btnAddPeople.visibility = View.VISIBLE
            gameFirebaseDatabase.createAcceptFriendRequest(
                currentUserId,
                currentUserName,
                currentUserImage,
                fromMentorId,
                "false"
            )
            handler.removeCallbacksAndMessages(null)
            if (isUiActive)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            gameFirebaseDatabase.deleteRequest(currentUserId)
        }

        binding.buttonAccept.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_ACCEPT_BUTTON)
            handler.removeCallbacksAndMessages(null)
            binding.notificationCard.visibility = View.INVISIBLE
            binding.btnAddPeople.visibility = View.INVISIBLE
            decisionDataViewModel?.addFavouritePracticePartner(
                AddFavouritePartner(
                    fromMentorId,
                    currentUserId,
                    currentUserId
                )
            )
            activity?.let {
                decisionDataViewModel?.fppData?.observe(it, Observer {
                    if (it.message == "done") {
                        gameFirebaseDatabase.deleteRequest(currentUserId)
                        gameFirebaseDatabase.createAcceptFriendRequest(
                            currentUserId,
                            currentUserName,
                            currentUserImage,
                            fromMentorId,
                            "true"
                        )
                    }
                })
            }
        }

        binding.butonDecline.setOnClickListener {
            binding.btnAddPeople.visibility = View.VISIBLE
            gameFirebaseDatabase.createAcceptFriendRequest(
                currentUserId,
                currentUserName,
                currentUserImage,
                fromMentorId,
                "false"
            )
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            handler.removeCallbacksAndMessages(null)
            if (isUiActive)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            gameFirebaseDatabase.deleteRequest(currentUserId)
        }

        try {
            handler.postDelayed({
                if (isUiActive)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
                gameFirebaseDatabase.deleteRequest(currentUserId)
                gameFirebaseDatabase.createAcceptFriendRequest(
                    currentUserId,
                    currentUserName,
                    currentUserImage,
                    fromMentorId,
                    "false"
                )
                binding.btnAddPeople.visibility = View.VISIBLE
            }, 10000)
        } catch (ex: Exception) {
        }

    }

    private fun againPlayGame() {
        gameFirebaseDatabase.deleteUserPlayAgainCollection(currentUserId)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    SearchingOpponentTeamFragmentFpp.newInstance(
                        callTimeCount ?: EMPTY,
                        UserDetails(partnerId, partnerName, partnerImage), currentUserTeamId
                    ), "Win"
                )
                ?.remove(this)
                ?.commit()
        }, 2000)
    }

    private fun getPlayAgainDataFromFirebase() {
        gameFirebaseDatabase.getPlayAgainAPiData(currentUserId, this@GameDecisionFragment)
    }

    private fun playAgainApiCall(playAgain: PlayAgain) {
        decisionDataViewModel?.playAgainWithSamePlayer(playAgain)
        activity?.let {
            decisionDataViewModel?.playAgainData?.observe(it, {
                if (it.message == "Both Member Added") {
                    getPlayAgainDataFromFirebase()
                }
            })
        }
    }

    override fun onPlayAgainNotificationFromApi(userName: String, userImage: String) {
        againPlayGame()
    }

    override fun onPartnerPlayAgainNotification(
        userName: String,
        userImage: String,
        mentorId: String
    ) {
        if (mentorId == currentUserId) {
            gameFirebaseDatabase.deletePlayAgainNotification(currentUserId)
            if (isUiActive)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationPlayAgain)
            binding.userNameForNotPlay.text = userName
            binding.txtMsgPlayAgain.text = "Click on Play Again to play another game with $userName"
            val imageUrl = userImage.replace("\n", EMPTY)
            binding.userImageForNotPaly.setUserImageOrInitials(imageUrl, userName, 30, isRound = true)

            binding.cancelNotification.setOnClickListener {
                handler.removeCallbacksAndMessages(null)
                if (isUiActive)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationPlayAgain)
                gameFirebaseDatabase.deletePlayAgainNotification(currentUserId)
            }

            try {
                handler.postDelayed({
                    if (isUiActive)
                        CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(
                            binding.notificationPlayAgain
                        )
                    gameFirebaseDatabase.deletePlayAgainNotification(currentUserId)
                }, 10000)
            } catch (ex: Exception) {
            }

        }
    }

    override fun onPartnerAcceptFriendRequest(
        userName: String,
        userImage: String,
        isAccept: String
    ) {
        binding.btnAddPeople.visibility = View.INVISIBLE
        if (isUiActive) {
            binding.buttonLinearLayout.visibility = View.INVISIBLE
            CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCard)
        }
        if (isAccept == "true") {
            handler.removeCallbacksAndMessages(null)
            binding.txtMsg2.text = "Accept your friend request"
            binding.userName.text = userName
            val imageUrl = userImage.replace("\n", EMPTY)
            binding.userImage.setUserImageOrInitials(imageUrl, userName, 30, isRound = true)
        } else {
            handler.removeCallbacksAndMessages(null)
            binding.txtMsg2.text = "Decline your friend request"
            binding.userName.text = userName
            val imageUrl = userImage.replace("\n", EMPTY)
            binding.userImage.setUserImageOrInitials(imageUrl, userName, 30, isRound = true)
        }

        binding.eee.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            if (isUiActive)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            gameFirebaseDatabase.deleteAcceptFppRequestNotification(currentUserId)
        }

        try {
            handler.postDelayed({
                if (isUiActive)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(
                        binding.notificationCard
                    )
                gameFirebaseDatabase.deleteAcceptFppRequestNotification(currentUserId)
            }, 10000)
        } catch (ex: Exception) {
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isUiActive = false
            gameFirebaseDatabase.deleteUserPlayAgainCollection(currentUserId)
        } catch (ex: Exception) {
        }
    }

    fun deleteAllFromFirestore(mentorId: String) {
        gameFirebaseDatabase.deleteAllData(mentorId)
        gameFirebaseDatabase.deleteMuteUnmute(mentorId)
        gameFirebaseDatabase.deletePartnerCutCard(currentUserTeamId ?: EMPTY)
        gameFirebaseDatabase.deleteAnimUser(partnerId ?: EMPTY)
        gameFirebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: EMPTY)
    }

    fun getSplitName(name: String?): String {
        return name?.split(" ")?.get(0).toString()
    }
}