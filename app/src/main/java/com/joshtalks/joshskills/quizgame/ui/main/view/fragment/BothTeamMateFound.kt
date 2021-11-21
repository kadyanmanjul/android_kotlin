package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentBothTeamMateFoundBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.ImageAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.BothTeamViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.BothTeamViewProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentTeamViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class BothTeamMateFound : Fragment(),P2pRtc.WebRtcEngineCallback {
    var startTime:String?=null
    private var roomId:String?=null
    private var userDetails : UserDetails? = null
    private var channelName:String?=null
    var bothTeamRepo : BothTeamRepo?=null
    var factory : BothTeamViewProviderFactory?=null
    var bothTeamViewModel:BothTeamViewModel?=null
    lateinit var binding: FragmentBothTeamMateFoundBinding
    var teamId1 :String?=null
    var teamId2 : String?=null
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

    private var currentUserId : String?=null
    private var engine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            startTime = it.getString("start_time")
            roomId = it.getString("room_id")
            userDetails = it.getParcelable("userDetails")
            channelName = it.getString("channelName")
        }
        setupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
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

        currentUserId = Mentor.getInstance().getUserId()
        getRoomData()
        moveFragment()

        try {
            engine = P2pRtc().initEngine(requireActivity())
        }catch (ex:Exception){
            Timber.d(ex)
        }
        onBackPress()
    }

    companion object {
        @JvmStatic
        fun newInstance(startTime: String,roomId:String,userDetails: UserDetails?, channelName: String) =
            BothTeamMateFound().apply {
                arguments = Bundle().apply {
                    putString("start_time",startTime)
                    putString("room_id",roomId)
                    putParcelable("userDetails",userDetails)
                    putString("channelName",channelName)
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
        bothTeamViewModel?.getRoomUserData(RandomRoomData(roomId?:"",currentUserId?:""))
        activity?.let {
               bothTeamViewModel?.roomUserData?.observe(it, Observer {
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


        if (team1UserId1 == currentUserId || team1UserId2 == currentUserId){

            val imageUrl1 = team1User1ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage3,imageUrl1)
            binding.userName3.text = team1User1Name

            val imageUrl2= team1User2ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage4,imageUrl2)
            binding.userName4.text = team1User2Name


            val imageUrl3 = team2User1ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage1,imageUrl3)
            binding.userName1.text = team2User1Name

            val imageUrl4= team2User2ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage2,imageUrl4)
            binding.userName2.text = team2User2Name

        }else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId){

            val imageUrl1 = team2User1ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage3,imageUrl1)
            binding.userName3.text = team1User1Name

            val imageUrl2= team2User2ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage4,imageUrl2)
            binding.userName4.text = team2User2Name

            val imageUrl3 = team1User1ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage1,imageUrl3)
            binding.userName1.text = team1User1Name

            val imageUrl4= team1User2ImageUrl?.replace("\n","")
            ImageAdapter.imageUrl(binding.userImage2,imageUrl4)
            binding.userName2.text = team1User2Name
        }
    }
    private fun moveFragment(){
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime :String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(R.id.container,
                    QuestionFragment.newInstance(roomId,startTime,"Favourite"),"SearchingOpponentTeam")
                ?.commit()
            fm?.popBackStack()
        }, 3000)
    }
    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
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

        //yaha hame phele check karna hai room id bani ya nahi agar ban chuki hai tu hame clear radius karna hai jsi
        // jis se room data or firebase vo user delete ho jaye
        //agar room nahi bana hai tu sirf user ko dlete karna hai
        yesBtn.setOnClickListener {
                bothTeamViewModel?.deleteUserRoomData(RandomRoomData(roomId?:"",currentUserId?:""))
                activity?.let {
                    bothTeamViewModel?.deleteData?.observe(it, Observer {
                        showToast(it.message)
                        dialog.dismiss()
                        AudioManagerQuiz.audioRecording.stopPlaying()
                        openChoiceScreen()
                        engine?.leaveChannel()
                        //callback.onPartnerLeave()
                    })
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
    private fun openChoiceScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                ChoiceFragnment.newInstance(),"BothTeamMate")
            ?.remove(this)
            ?.commit()
        fm?.popBackStack()
    }

    override fun onPartnerLeave() {
        super.onPartnerLeave()
        showToast("Partner Leave The Channel")
    }
}