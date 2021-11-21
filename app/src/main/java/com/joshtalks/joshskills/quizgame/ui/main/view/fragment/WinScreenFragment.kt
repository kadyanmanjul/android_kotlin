package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentWinScreenBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.QuestionRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SaveRoomRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.ImageAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SaveRoomDataViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SaveRoomDataViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine


class WinScreenFragment : Fragment() {
    private lateinit var binding : FragmentWinScreenBinding
    private var marks: String? = null
    private var roomId: String? = null
    private var teamId : String? = null
    private var winnerTeamStatus : Boolean?=null
    private var saveRoomRepo : SaveRoomRepo?=null
    private var factory: SaveRoomDataViewProviderFactory? = null
    private var saveRoomDataViewModel: SaveRoomDataViewModel? = null
    private var currentUserId :String?=null
    private var opponentTeamMarks:String?=null

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
    private var callTimeCount:String?=null
    private var fromType:String?=null
    private var engine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
           marks = it.getString("team_score")
           opponentTeamMarks = it.getString("opponent_team_marks")
           roomId = it.getString("room_id")
           teamId = it.getString("team_id")
           callTimeCount =it.getString("callTime")
           fromType=it.getString("fromType")
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
        binding.lifecycleOwner = this
        binding.clickHandler = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUserId = Mentor.getInstance().getUserId()

        showRoomUserData()
        engine = P2pRtc().initEngine(requireActivity())

        binding.currentUserMarks.text = marks
        binding.opponentTeamMarks.text = opponentTeamMarks

        onBackPress()

        //ek condition else if ayega jab match tie hoga mtlb dono ke marks same hoge

        when {
            marks?.toInt()?:0 > opponentTeamMarks?.toInt()?:0 -> {
                winnerTeamStatus = true
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(5)).toString()
                binding.opponentUserTotalMarks.text = (opponentTeamMarks?.toInt()?.plus(0)).toString()
                binding.winningPointCurrent.text = "5"
                binding.winningPointOpponent.text = "0"
                binding.txtOpponentWin.visibility = View.VISIBLE
                binding.txtOpponentWin.text = "You Won!"
                binding.conglutions.visibility = View.VISIBLE
                binding.winImg11.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_congratulations)
                //yaha congulation wala or won ka icon lagana hai
            }
            marks?.toInt()?:0 == opponentTeamMarks?.toInt()?:0 -> {
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(0)).toString()
                binding.opponentUserTotalMarks.text = (opponentTeamMarks?.toInt()?.plus(0)).toString()
                binding.winningPointCurrent.text = "0"
                binding.winningPointOpponent.text = "0"
                binding.conglutions.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_matcg_draw)
            }
            else -> {
                winnerTeamStatus = false
                binding.currentUserTotalMarks.text = (marks?.toInt()?.plus(0)).toString()
                binding.opponentUserTotalMarks.text = (opponentTeamMarks?.toInt()?.plus(5)).toString()
                binding.winningPointCurrent.text = "0"
                binding.winningPointOpponent.text = "5"
                binding.txtWinner.text = "Winner"
                binding.txtOpponentWin.visibility = View.VISIBLE
                binding.txtOpponentWin.text = "Opponent Won!"
                binding.conglutions.visibility = View.VISIBLE
                binding.conglutions.setImageResource(R.drawable.ic_opponentwon)
                binding.winImg.visibility = View.VISIBLE
                binding.txtWinner.visibility = View.VISIBLE
                binding.conglutions.visibility = View.VISIBLE
                //yaha opponent won wala or won ka icon lagana hai
            }
        }

        val time:Int? = callTimeCount?.toInt()?.div(1000)
        setUpViewModel(SaveRoomDetails(roomId,teamId,marks,winnerTeamStatus, time?.toString()))

        activity?.let {
            saveRoomDataViewModel?.saveRoomDetailsData?.observe(it, Observer {
                Log.d("success_save", "onViewCreated: "+it.message)
            })
        }

        binding.btnMakeNewTeam.setOnClickListener {
            makeNewTeam()
        }

        binding.btnPlayAgain.setOnClickListener {

        }
    }

    companion object {
        @JvmStatic
        fun newInstance(marks: String,opponentTeamMarks:String, roomId: String?,teamId:String?,callTime:String?,fromType:String) =
            WinScreenFragment().apply {
                arguments = Bundle().apply {
                    putString("team_score", marks)
                    putString("opponent_team_marks",opponentTeamMarks)
                    putString("room_id", roomId)
                    putString("team_id", teamId)
                    putString("callTime",callTime)
                    putString("fromType",fromType)
                }
            }
    }
    fun setUpViewModel(saveRoomDetails: SaveRoomDetails){
        saveRoomDataViewModel?.saveRoomDetails(saveRoomDetails)
    }
    fun setRoomUsersData(){
        saveRoomRepo = SaveRoomRepo()
        factory = SaveRoomDataViewProviderFactory(requireActivity().application, saveRoomRepo!!)
        saveRoomDataViewModel = ViewModelProvider(this, factory!!).get(SaveRoomDataViewModel::class.java)
        saveRoomDataViewModel?.getRoomUserDataTemp(RandomRoomData(
            roomId ?: "",
            currentUserId ?: ""
        ))
    }
    fun showRoomUserData() {
        activity?.let {
            saveRoomDataViewModel?.roomUserDataTemp?.observe(it, Observer {
                initializeUsersTeamsData(it.teamData)
            })
        }
    }
    fun initializeUsersTeamsData(teamsData: TeamsData) {
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
            //yaha par ham jo current user hai usko niche set karte hai or uske partner ko bhi

            currentUserTeamId = team1Id
            opponentTeamId = team2Id

            val imageUrl1 = team1User1ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team1UserImage1, imageUrl1)
            binding.team1User1Name.text = team1User1Name

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team1UserImage2, imageUrl2)
            binding.team1User2Name.text = team1User2Name

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team2UserImage1, imageUrl3)
            binding.team2User1Name.text = team2User1Name

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team2UserImage2, imageUrl4)
            binding.team2User2Name.text = team2User2Name

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            val imageUrl1 = team2User1ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team1UserImage1, imageUrl1)
            binding.team1User1Name.text = team2User1Name

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team1UserImage2, imageUrl2)
            binding.team1User2Name.text = team2User2Name

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team2UserImage1, imageUrl3)
            binding.team2User1Name.text = team1User1Name

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            ImageAdapter.imageUrl(binding.team2UserImage2, imageUrl4)
            binding.team2User2Name.text = team1User2Name
        }
    }
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showDialog()
                }
            })
    }
    private fun showDialog() {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
           deleteData(dialog)
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    fun openFavouritePartnerScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragnment.newInstance(), "Win"
            )
            ?.remove(this)
            ?.commit()
        fm?.popBackStack()
    }
    fun deleteData(dialog: Dialog){
        if (fromType == "Random"){
            saveRoomDataViewModel?.getClearRadius(RandomRoomData(roomId?:"",currentUserId?:""))
            activity?.let {
                saveRoomDataViewModel?.clearRadius?.observe(it, {
                    dialog.dismiss()
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    engine?.leaveChannel()
                    openFavouritePartnerScreen()
                })
            }
        }else{
            saveRoomDataViewModel?.deleteUserRoomData(RandomRoomData(roomId?:"",currentUserId?:""))
            activity?.let {
                saveRoomDataViewModel?.deleteData?.observe(it, Observer {
                    dialog.dismiss()
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    engine?.leaveChannel()
                    openFavouritePartnerScreen()
                })
            }
        }
    }
    fun makeNewTeam(){
        if (fromType == "Random"){
            saveRoomDataViewModel?.getClearRadius(RandomRoomData(roomId?:"",currentUserId?:""))
            activity?.let {
                saveRoomDataViewModel?.clearRadius?.observe(it, {
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    engine?.leaveChannel()
                    openFavouritePartnerScreen()
                })
            }
        }else{
            saveRoomDataViewModel?.deleteUserRoomData(RandomRoomData(roomId?:"",currentUserId?:""))
            activity?.let {
                saveRoomDataViewModel?.deleteData?.observe(it, Observer {
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    engine?.leaveChannel()
                    openFavouritePartnerScreen()
                })
            }
        }
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback{
        override fun onChannelJoin() {
            super.onChannelJoin()
        }

        override fun onConnect(callId: String) {
            super.onConnect(callId)
        }

        override fun onDisconnect(callId: String?, channelName: String?) {
            super.onDisconnect(callId, channelName)
        }

        override fun onSpeakerOff() {
            super.onSpeakerOff()
        }

        override fun onNetworkLost() {
            super.onNetworkLost()
        }

        override fun onPartnerLeave() {
            super.onPartnerLeave()
        }
    }
}