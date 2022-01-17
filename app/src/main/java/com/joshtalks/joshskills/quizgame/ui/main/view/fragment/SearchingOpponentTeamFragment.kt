package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.MaskFilter
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentSearchingOpponentTeamBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.ImageAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentTeamViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.SearchOpponentViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.MyBounceInterpolator
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

//Channel Name = team_id
const val USER_DATA:String ="user_data"
class SearchingOpponentTeamFragment : Fragment(), FirebaseDatabase.OnNotificationTrigger,
        P2pRtc.WebRtcEngineCallback{

    lateinit var binding:FragmentSearchingOpponentTeamBinding
    var startTime:String?=null
    var channelName:String?=null
    var repository : SearchOpponentRepo?=null
    var factory : SearchOpponentViewProviderFactory?=null
    var searchOpponentTeamViewModel:SearchOpponentTeamViewModel?=null
    var userDetails : UserDetails?=null
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    var roomId:String?=null
    private var teamId1:String?=null
    private var teamId2:String?=null

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

    private var currentUserTeamId: String? = null
    private var opponentTeamId: String? = null

    private var engine: RtcEngine? = null
    private var timer: CountDownTimer? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserId= Mentor.getInstance().getUserId()
        arguments?.let {
            startTime = it.getString(START_TIME)
            userDetails = it.getParcelable(USER_DATA)
            channelName = it.getString(CHANNEL_NAME)
            currentUserTeamId = channelName
            Log.d("channel_name", "onCreate: "+currentUserTeamId)

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

        setCurrentUserData()
        setTeamMateData(userDetails)
        startTimer()
        activity?.let {
            searchOpponentTeamViewModel?.roomData?.observe(it, Observer {
            })
        }

        try {
            firebaseDatabase.getCurrentUserRoomId(Mentor.getInstance().getUserId(),this)
        }catch (ex:Exception){
            Timber.d(ex)
        }

        try {
           // engine = P2pRtc().initEngine(requireActivity())
            engine = P2pRtc().getEngineObj()
            P2pRtc().addListener(callback)
        }catch (ex:Exception){
            Timber.d(ex)
        }

        onBackPress()
        dipDown(binding.vs1)
    }
    companion object {
        //mujhe yaha team id bhi lana padega piche se jis se me add to room api call kar saku or firebase se data la saku
        @JvmStatic
        fun newInstance(param1: String,userDetails:UserDetails?,channelName: String?) =
            SearchingOpponentTeamFragment().apply {
                arguments = Bundle().apply {
                    putString(START_TIME,param1)
                    putParcelable(USER_DATA,userDetails)
                    putString(CHANNEL_NAME,channelName)
                    Log.d("channel_name", "newInstance: $channelName")
                }
            }
    }
    private fun setCurrentUserData() {
        binding.team1User1Name.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl= Mentor.getInstance().getUser()?.photo?.replace("\n","")
        binding.team1UserImage1.setUserImageOrInitials(imageUrl,Mentor.getInstance().getUser()?.firstName?:"",30,isRound = true)
       // ImageAdapter.imageUrl(binding.team1UserImage1,imageUrl)
    }
    private fun setTeamMateData(userDetails: UserDetails?){
        binding.team1User2Name.text = userDetails?.name
        val imageUrl=userDetails?.imageUrl?.replace("\n","")
        binding.team1UserImage2.setUserImageOrInitials(imageUrl,userDetails?.name?:"",30,isRound = true)
       // ImageAdapter.imageUrl(binding.team1UserImage2,imageUrl)
    }
    private fun setupViewModel() {
        repository = SearchOpponentRepo()
        factory = activity?.application?.let { SearchOpponentViewProviderFactory(it, repository!!) }
        searchOpponentTeamViewModel = factory?.let {
            ViewModelProvider(this, it).get(SearchOpponentTeamViewModel::class.java)
        }
        searchOpponentTeamViewModel?.addToRoomData(ChannelName(channelName))
    }
    override fun onStart() {
        super.onStart()
        call_time.visibility = View.VISIBLE
        call_time.base = SystemClock.elapsedRealtime().minus(startTime?.toLong()!!)
        call_time.start()
    }
    override fun onNotificationForInvitePartner(channelName: String, fromUserId: String, fromUserName: String, fromUserImage: String)
    {

    }
    override fun onNotificationForPartnerNotAccept(userName: String?, userImageUrl: String, fromUserId: String,declinrUserID:String)
    {
    }
    override fun onNotificationForPartnerAccept(channelName: String?, timeStamp: String, isAccept: String, opponentMemberId: String, mentorId: String) {
    }

    //yaha hame room wali list me current user ki room id milegi
    override fun onGetRoomId(currentUserRoomID: String?,mentorId: String) {
        roomId = currentUserRoomID
        Log.d("response_room", "onGetRoomId: "+currentUserRoomID)
            if (currentUserRoomID!=null){
                searchOpponentTeamViewModel?.getRoomUserData(RandomRoomData(currentUserRoomID,mentorId))
                activity?.let {
                    searchOpponentTeamViewModel?.roomUserData?.observe(it, Observer {
                        initializeUsersTeamsData(it.teamData)
                        timer?.cancel()
                        timer?.onFinish()
                    })
                }
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


        if (team1UserId1 ==  currentUserId || team1UserId2 == currentUserId) {
            //yaha par ham jo current user hai usko niche set karte hai or uske partner ko bhi

            currentUserTeamId = team1Id
            opponentTeamId = team2Id

//            val imageUrl1 = team1User1ImageUrl?.replace("\n", "")
//            ImageAdapter.imageUrl(binding.team1UserImage1, imageUrl1)
//            binding.team1User1Name.text = team1User1Name

            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team1UserImage2, imageUrl2)
            binding.team1UserImage2.setUserImageOrInitials(imageUrl2,team1User2Name?:"",30,true)
            binding.team1User2Name.text = team1User2Name

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
           // ImageAdapter.imageUrl(binding.team2UserImage1, imageUrl3)
            binding.team2UserImage1.setUserImageOrInitials(imageUrl3,team2User1Name?:"",30,true)
            binding.team2User1Name.text = team2User1Name

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team2UserImage2, imageUrl4)
            binding.team2UserImage2.setUserImageOrInitials(imageUrl4,team2User2Name?:"",30,true)
            binding.team2User2Name.text = team2User2Name

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            currentUserTeamId = team2Id
            opponentTeamId = team1Id

//            val imageUrl1 = team2User1ImageUrl?.replace("\n", "")
//            ImageAdapter.imageUrl(binding.team1UserImage1, imageUrl1)
//            binding.team1User1Name.text = team2User1Name

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team1UserImage2, imageUrl2)
            binding.team1UserImage2.setUserImageOrInitials(imageUrl2,team2User2Name?:"",30,true)
            binding.team1User2Name.text = team2User2Name

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team2UserImage1, imageUrl3)
            binding.team2UserImage1.setUserImageOrInitials(imageUrl3,team1User1Name?:"",30,true)
            binding.team2User1Name.text = team1User1Name

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            //ImageAdapter.imageUrl(binding.team2UserImage2, imageUrl4)
            binding.team2UserImage2.setUserImageOrInitials(imageUrl4,team1User2Name?:"",30,true)
            binding.team2User2Name.text = team1User2Name

        }
    }
    override fun onShowAnim(mentorId: String,isCorrect: String,c:String,m:String) {
    }
    private fun moveFragment(){
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime :String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(R.id.container,
                    BothTeamMateFound.newInstance(startTime,roomId?:"",userDetails,channelName?:""),"BothTeamMateFound")
                ?.commit()
            fm?.popBackStack()
        },4000)
    }
    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
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

        //yaha hame phele check karna hai room id bani ya nahi agar ban chuki hai tu hame clear radius karna hai jsi
        // jis se room data or firebase vo user delete ho jaye
        //agar room nahi bana hai tu sirf user ko delete karna hai
        yesBtn.setOnClickListener {
            if (roomId!=null){
                Log.d("room_id_data", "showDialog: "+roomId +" "+currentUserId)
                searchOpponentTeamViewModel?.deleteUserRoomData(SaveCallDurationRoomData(roomId?:"",currentUserId?:"",currentUserTeamId?:"",startTime?:""))
                activity?.let {
                    searchOpponentTeamViewModel?.deleteData?.observe(it, Observer {
                        dialog.dismiss()
                        AudioManagerQuiz.audioRecording.stopPlaying()
                        engine?.leaveChannel()
                        binding.callTime.stop()
                        timer?.cancel()
                        openChoiceScreen()
                    })
                }
            }else{
                searchOpponentTeamViewModel?.deleteUserAndTeamData(TeamDataDelete(currentUserTeamId?:"",currentUserId?:""))
                activity?.let {
                    searchOpponentTeamViewModel?.deleteData?.observe(it, Observer {
                        dialog.dismiss()
                        AudioManagerQuiz.audioRecording.stopPlaying()
                        engine?.leaveChannel()
                        binding.callTime.stop()
                        timer?.cancel()
                        openChoiceScreen()
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
    private fun openChoiceScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                ChoiceFragnment.newInstance(),"TeamMate")
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
                if (roomId!=null){
                    moveFragment()
                }else {
                    showToast(NO_OPPONENT_FOUND)
                    deleteTeamData()
                }
            }
        }.start()
    }
    private fun deleteTeamData(){
        searchOpponentTeamViewModel?.deleteUserAndTeamData(TeamDataDelete(currentUserTeamId?:"",currentUserId?:""))
        activity?.let {
            searchOpponentTeamViewModel?.deleteData?.observe(it, Observer {
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

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback{

        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    binding.team1User2Name.alpha = 0.5f
                    binding.team1UserImage2Shadow.visibility = View.VISIBLE
                }
            }catch (ex:Exception){
                Log.d("error_res", "onPartnerLeave: "+ex.message?:"")
            }
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