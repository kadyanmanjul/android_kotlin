package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LEAVE_THE_GAME
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentSearchingOpponentTeamBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentTeamViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


//Channel Name = team_id
const val USER_DATA: String = "user_data"

class SearchingOpponentTeamFragment : Fragment(), FirebaseDatabase.OnNotificationTrigger,
    P2pRtc.WebRtcEngineCallback {

    lateinit var binding: FragmentSearchingOpponentTeamBinding
    var startTime: String? = null
    var channelName: String? = null

    var searchOpponentTeamViewModel: SearchOpponentTeamViewModel? = null

    var firebaseTemp = FirebaseTemp()
    var userDetails: UserDetails? = null
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    var roomId: String? = null
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
    var factory: SearchOpponentViewProviderFactory? = null

    private var currentUserId = Mentor.getInstance().getId()

    private var currentUserTeamId: String? = null
    private var opponentTeamId: String? = null

    private var engine: RtcEngine? = null
    private var timer: CountDownTimer? = null

    //private var isUiActive: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            startTime = it.getString(START_TIME)
            userDetails = it.getParcelable(USER_DATA)
            channelName = it.getString(CHANNEL_NAME)
            currentUserTeamId = channelName
            Timber.d(currentUserTeamId)
        }
        setupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_searching_opponent_team,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this
        binding.callTime.visibility = View.VISIBLE
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
        setCurrentUserData()
        setTeamMateData(userDetails)
        startTimer()
        activity?.let {
            searchOpponentTeamViewModel?.roomData?.observe(it, Observer {
            })
        }

        try {
            firebaseDatabase.getCurrentUserRoomId(Mentor.getInstance().getId(), this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        try {
            engine = P2pRtc().getEngineObj()
            P2pRtc().addListener(callback)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        onBackPress()
        UtilsQuiz.dipDown(binding.vs1, requireActivity())
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, userDetails: UserDetails?, channelName: String?) =
            SearchingOpponentTeamFragment().apply {
                arguments = Bundle().apply {
                    putString(START_TIME, param1)
                    putParcelable(USER_DATA, userDetails)
                    putString(CHANNEL_NAME, channelName)
                    Timber.d(channelName)
                }
            }
    }

    private fun setCurrentUserData() {
        binding.team1User1Name.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl = Mentor.getInstance().getUser()?.photo?.replace("\n", "")
        binding.team1UserImage1.setUserImageOrInitials(
            imageUrl,
            Mentor.getInstance().getUser()?.firstName ?: "",
            30,
            isRound = true
        )
        // ImageAdapter.imageUrl(binding.team1UserImage1,imageUrl)
    }

    private fun setTeamMateData(userDetails: UserDetails?) {
        binding.team1User2Name.text = UtilsQuiz.getSplitName(userDetails?.name)
        val imageUrl = userDetails?.imageUrl?.replace("\n", "")
        binding.team1UserImage2.setUserImageOrInitials(
            imageUrl,
            userDetails?.name ?: "",
            30,
            isRound = true
        )
        // ImageAdapter.imageUrl(binding.team1UserImage2,imageUrl)
    }

    private fun setupViewModel() {
        factory = activity?.application?.let { SearchOpponentViewProviderFactory(it) }
        searchOpponentTeamViewModel = factory?.let {
            ViewModelProvider(this, it).get(SearchOpponentTeamViewModel::class.java)
        }
        searchOpponentTeamViewModel?.addToRoomData(
            ChannelName(
                channelName,
                Mentor.getInstance().getId()
            )
        )
    }

    override fun onStart() {
        super.onStart()
        call_time.visibility = View.VISIBLE
        call_time.base = SystemClock.elapsedRealtime().minus(startTime?.toLong()!!)
        call_time.start()
    }

    override fun onGetRoomId(currentUserRoomID: String?, mentorId: String) {
        roomId = currentUserRoomID
        Timber.d(currentUserRoomID)
        if (currentUserRoomID != null) {
            searchOpponentTeamViewModel?.getRoomUserData(
                RandomRoomData(
                    currentUserRoomID,
                    mentorId
                )
            )
            activity?.let {
                searchOpponentTeamViewModel?.roomUserData?.observe(it, Observer {
                    initializeUsersTeamsData(it.teamData)
                    binding.image9.pauseAnimation()
                    timer?.cancel()
                    timer?.onFinish()
                })
            }
        }
    }


    private fun initializeUsersTeamsData(teamsData: TeamsData?) {
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

            currentUserTeamId = team1Id
            opponentTeamId = team2Id

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = UtilsQuiz.getSplitName(team1User2Name)

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
            opponentTeamId = team1Id

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = UtilsQuiz.getSplitName(team2User2Name)

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

    override fun onShowAnim(mentorId: String, isCorrect: String, c: String, m: String) {
    }

    private fun moveFragment() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime: String =
                (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    BothTeamMateFound.newInstance(
                        startTime,
                        roomId ?: "",
                        userDetails,
                        channelName ?: ""
                    ), "BothTeamMateFound"
                )
                ?.commit()
            fm?.popBackStack()
        }, 4000)
    }

    private fun onBackPress() {
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
        searchOpponentTeamViewModel?.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: "",
                startTime.toInt().div(1000).toString(),
                currentUserId
            )
        )
        if (roomId != null) {
            searchOpponentTeamViewModel?.deleteUserRoomData(
                SaveCallDurationRoomData(
                    roomId ?: "",
                    currentUserId,
                    currentUserTeamId ?: "",
                    startTime
                )
            )
            activity?.let {
                searchOpponentTeamViewModel?.deleteData?.observe(it, {
                    if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_FPP){
                        activity?.let {
                            searchOpponentTeamViewModel?.saveCallDuration?.observe(it, {
                                if (it.message == CALL_DURATION_RESPONSE) {
                                    val points = it.points
                                    if (points.toInt() >= 1){
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
                                    timer?.cancel()
                                    openChoiceScreen()
                                }
                            })
                        }
                    }
                })
            }
        } else {
            searchOpponentTeamViewModel?.deleteUserAndTeamData(
                TeamDataDelete(
                    currentUserTeamId ?: "", currentUserId
                )
            )
            activity?.let {
                searchOpponentTeamViewModel?.saveCallDuration?.observe(it, Observer {
                    if (it.message == CALL_DURATION_RESPONSE) {
                        firebaseTemp.changeUserStatus(currentUserId, ACTIVE)
                        val points = it.points
                        if (points.toInt() >= 1){
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
                        timer?.cancel()
                        openChoiceScreen()
                    }
                })
            }
        }
    }

    private fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "TeamMate"
            )
            ?.remove(this)
            ?.commit()
        fm?.popBackStack()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //var seconds = (millisUntilFinished / 1000).toInt()
                //seconds %= 60
                //binding.count.text = String.format("%2d", seconds)
            }

            override fun onFinish() {
                if (roomId != null) {
                    moveFragment()
                } else {
                    try {
                        Toast.makeText(context, NO_OPPONENT_FOUND, Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) { }
                    deleteTeamData()
                }
            }
        }.start()
    }

    private fun deleteTeamData() {
        searchOpponentTeamViewModel?.deleteUserAndTeamData(
            TeamDataDelete(
                currentUserTeamId ?: "",
                currentUserId
            )
        )
        activity?.let {
            searchOpponentTeamViewModel?.deleteData?.observe(it, Observer {
                firebaseTemp.changeUserStatus(currentUserId, ACTIVE)
                AudioManagerQuiz.audioRecording.stopPlaying()
                engine?.leaveChannel()
                binding.callTime.stop()
                openChoiceScreen()
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        roomId = null
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback {
        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    binding.callTime.stop()
                    PrefManager.put(USER_LEAVE_THE_GAME, true)
                    binding.team1User2Name.alpha = 0.5f
                    binding.team1UserImage2Shadow.visibility = View.VISIBLE

                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }
}