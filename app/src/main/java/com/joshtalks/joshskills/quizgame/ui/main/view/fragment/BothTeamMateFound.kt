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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LEAVE_THE_GAME
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentBothTeamMateFoundBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.BothTeamViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.BothTeamViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val START_TIME: String = "start_time"
const val ROOM_ID: String = "room_id"
const val CHANNEL_NAME = "channelName"
const val USER_DETAILS: String = "userDetails"
const val MESSAGE = "Your partner left the you have to play alone"

class BothTeamMateFound : Fragment(), P2pRtc.WebRtcEngineCallback {
    var startTime: String? = null
    private var roomId: String? = null
    private var userDetails: UserDetails? = null
    private var channelName: String? = null
    var bothTeamRepo: BothTeamRepo? = null
    var factory: BothTeamViewProviderFactory? = null
    var bothTeamViewModel: BothTeamViewModel? = null
    lateinit var binding: FragmentBothTeamMateFoundBinding
    var teamId1: String? = null
    var teamId2: String? = null
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

    private var currentUserId: String? = null
    private var engine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            startTime = it.getString(START_TIME)
            roomId = it.getString(ROOM_ID)
            userDetails = it.getParcelable(USER_DETAILS)
            channelName = it.getString(CHANNEL_NAME)
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
                R.layout.fragment_both_team_mate_found,
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
        binding.container.setBackgroundColor(Color.WHITE);
        if (PrefManager.getBoolValue(USER_LEAVE_THE_GAME)) {
            binding.userName4.alpha = 0.5f
            binding.userImage4Shadow.visibility = View.VISIBLE
        }
        currentUserId = Mentor.getInstance().getUserId()
        getRoomData()
        moveFragment()

        try {
            engine = P2pRtc().initEngine(requireActivity())
            P2pRtc().addListener(callback)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
        onBackPress()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            startTime: String,
            roomId: String,
            userDetails: UserDetails?,
            channelName: String
        ) =
            BothTeamMateFound().apply {
                arguments = Bundle().apply {
                    putString(START_TIME, startTime)
                    putString(ROOM_ID, roomId)
                    putParcelable(USER_DETAILS, userDetails)
                    putString(CHANNEL_NAME, channelName)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        call_time.visibility = View.VISIBLE
        call_time.base = SystemClock.elapsedRealtime().minus(startTime?.toLong()!!)
        call_time.start()
    }

    private fun setupViewModel() {
        bothTeamRepo = BothTeamRepo()
        factory = activity?.application?.let { BothTeamViewProviderFactory(it, bothTeamRepo!!) }
        bothTeamViewModel = factory?.let {
            ViewModelProvider(this, it).get(BothTeamViewModel::class.java)
        }
    }

    private fun getRoomData() {
        bothTeamViewModel?.getRoomUserData(RandomRoomData(roomId ?: "", currentUserId ?: ""))
        activity?.let {
            bothTeamViewModel?.roomUserData?.observe(it, {
                initializeUsersTeamsData(it.teamData)
            })
        }
    }

    private fun initializeUsersTeamsData(teamsData: TeamsData) {
        team1Id = teamsData.team1Id
        team2Id = teamsData.team2Id

        usersInTeam1 = teamsData.usersInTeam1
        usersInTeam2 = teamsData.usersInTeam2

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

            val imageUrl1 = team1User1ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.userImage3,imageUrl1)
            binding.userImage3.setUserImageOrInitials(imageUrl1, team1User1Name ?: "", 30, true)
            binding.userName3.text = team1User1Name

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            // ImageAdapter.imageUrl(binding.userImage4,imageUrl2)
            binding.userImage4.setUserImageOrInitials(imageUrl2, team1User2Name ?: "", 30, true)
            binding.userName4.text = team1User2Name

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage1,imageUrl3)
            binding.userImage1.setUserImageOrInitials(imageUrl3, team2User1Name ?: "", 30, true)
            binding.userName1.text = team2User1Name

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage2,imageUrl4)
            binding.userImage2.setUserImageOrInitials(imageUrl4, team2User2Name ?: "", 30, true)
            binding.userName2.text = team2User2Name

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            currentUserTeamId = team2Id

            val imageUrl1 = team2User1ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage3,imageUrl1)
            binding.userImage3.setUserImageOrInitials(imageUrl1, team2User1Name ?: "", 30, true)
            binding.userName3.text = team2User1Name

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage4,imageUrl2)
            binding.userImage4.setUserImageOrInitials(imageUrl2, team2User2Name ?: "", 30, true)
            binding.userName4.text = team2User2Name

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage1,imageUrl3)
            binding.userImage1.setUserImageOrInitials(imageUrl3, team1User1Name ?: "", 30, true)
            binding.userName1.text = team1User1Name

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.userImage2,imageUrl4)
            binding.userImage2.setUserImageOrInitials(imageUrl4, team1User2Name ?: "", 30, true)
            binding.userName2.text = team1User2Name
        }
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
                    QuestionFragment.newInstance(roomId, startTime, "Favourite"),
                    "SearchingOpponentTeam"
                )
                ?.commit()
            fm?.popBackStack()
        }, 3000)
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
        bothTeamViewModel?.deleteUserRoomData(
            SaveCallDurationRoomData(
                roomId ?: "",
                currentUserId ?: "",
                currentUserTeamId ?: "",
                startTime ?: ""
            )
        )
        bothTeamViewModel?.saveCallDuration(
            SaveCallDuration(
                channelName ?: "",
                startTime.toInt().div(1000).toString(),
                currentUserId ?: ""
            )
        )
        activity?.let {
            bothTeamViewModel?.saveCallDuration?.observe(it, {
                if (it.message == CALL_DURATION_RESPONSE) {
                    val points = it.points
                    lifecycleScope.launch(Dispatchers.Main) {
                        UtilsQuiz.showSnackBar(
                            binding.container,
                            Snackbar.LENGTH_SHORT,
                            "You earned +$points for speaking in English"
                        )
                    }
                    engine?.leaveChannel()
                    binding.callTime.stop()
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openChoiceScreen()
                }
            })
        }
    }

    private fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "BothTeamMate"
            )
            ?.remove(this)
            ?.commit()
        fm?.popBackStack()
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback {
        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    PrefManager.put(USER_LEAVE_THE_GAME, true)
                    binding.userName4.alpha = 0.5f
                    binding.userImage4Shadow.visibility = View.VISIBLE
                }
            } catch (ex: Exception) {
                //  showToast(ex.message?:"")
            }
        }
    }
}