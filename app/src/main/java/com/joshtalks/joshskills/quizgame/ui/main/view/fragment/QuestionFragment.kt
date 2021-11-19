package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.*
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.TriangleEdgeTreatment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentQuestionBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.repository.QuestionRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.ImageAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionViewModel
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.android.synthetic.main.fragment_question.*
import kotlinx.android.synthetic.main.fragment_question.call_time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestionFragment : Fragment(),FirebaseDatabase.OnNotificationTrigger,FirebaseDatabase.OnAnimationTrigger {
    private lateinit var binding: FragmentQuestionBinding
    private var position: Int = 0
    private var questionSize :Int =0
    private lateinit var question: QuestionResponse
    private var timer: CountDownTimer? = null
    private var givenTeamId: String? = null
    private var currentUserId: String? = null

    private var questionRepo: QuestionRepo? = null
    private var factory: QuestionProviderFactory? = null
    private var questionViewModel: QuestionViewModel? = null
    private var choiceValue: String? = null

    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()

    private var isCorrect: String? = null

    private var roomId: String? = null

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        currentUserId = Mentor.getInstance().getUserId()
        arguments?.let {
            roomId = it.getString("roomId")
            callTimeCount = it.getString("callTime")
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
                R.layout.fragment_question,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this

        binding.callTime.start()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("current_uid", "onViewCreated: " + currentUserId)
        scaleAnimationForTeam1(binding.imagesTeam1)
        scaleAnimationForTeam2(binding.imagesTeam2)
        animationForText(binding.txtLayout1)
        animationForText(binding.txtLayout2)
        slideAnimationLToR()
        slideAnimationRToL()
        showRoomUserData()
        onBackPress()
        animationForText(binding.roundNumber)
        activity?.let {
            try {
                questionViewModel?.questionData?.observe(it, Observer {
                    Log.d("response_que", "onViewCreated: " + it)
                    question = it
                    getQuestions()
                })
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
        binding.card1.setOnClickListener {
            drawTriangleOnCard(binding.card1)
            binding.progress1.pauseProgress()
            selectOptionCheck(
                currentUserId ?: "",
                team1Id,
                team2Id,
                roomId ?: "",
                0
            )
            getSelectOptionWithAnim(binding.answer1.text.toString(), 0)
        }
        binding.card2.setOnClickListener {
            drawTriangleOnCard(binding.card2)
            binding.progress1.pauseProgress()
            selectOptionCheck(
                currentUserId ?: "",
                team1Id,
                team2Id,
                roomId ?: "",
                1
            )

            getSelectOptionWithAnim(binding.answer2.text.toString(), 1)
        }
        binding.card3.setOnClickListener {
            drawTriangleOnCard(binding.card3)
            binding.progress1.pauseProgress()
            selectOptionCheck(
                currentUserId ?: "",
                team1Id,
                team2Id,
                roomId ?: "",
                2
            )

            getSelectOptionWithAnim(binding.answer3.text.toString(), 2)
        }
        binding.card4.setOnClickListener {
            drawTriangleOnCard(binding.card4)
            binding.progress1.pauseProgress()
            selectOptionCheck(
                currentUserId ?: "",
                team1Id,
                team2Id,
                roomId ?: "",
                3
            )

            getSelectOptionWithAnim(binding.answer4.text.toString(), 3)
        }
    }
    fun drawTriangleOnCard(view : View){
        val triangleEdgeTreatment = TriangleEdgeTreatment(30f, true)
        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setLeftEdge(triangleEdgeTreatment)
            .build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
        shapeDrawable.setCornerSize(3f)
        shapeDrawable.fillColor = ColorStateList.valueOf(resources.getColor(R.color.white))
        ViewCompat.setBackground(view, shapeDrawable)
    }
    fun drawTriangleOnCardRight(view : View){
        val triangleEdgeTreatment = TriangleEdgeTreatment(30f, true)
        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setLeftEdge(triangleEdgeTreatment)
            .setRightEdge(triangleEdgeTreatment)
            .build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
        shapeDrawable.setCornerSize(3f)
        shapeDrawable.fillColor = ColorStateList.valueOf(resources.getColor(R.color.white))
        ViewCompat.setBackground(view, shapeDrawable)
    }
    fun makeAgainCardSquare(view : View){
        val triangleEdgeTreatment = TriangleEdgeTreatment(0f, true)
        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setLeftEdge(triangleEdgeTreatment)
            .setRightEdge(triangleEdgeTreatment)
            .build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
        shapeDrawable.setCornerSize(3f)
        shapeDrawable.fillColor = ColorStateList.valueOf(resources.getColor(R.color.white))
        ViewCompat.setBackground(view, shapeDrawable)
    }
    fun slideAnimationLToR(){
        val relativeAnimLToR = AnimationUtils.loadAnimation(context, R.anim.hs__slide_in_from_left)
        binding.relativeAnim.startAnimation(relativeAnimLToR)
    }
    fun slideAnimationRToL(){
        val relativeAnimRToL = AnimationUtils.loadAnimation(context, R.anim.hs__slide_in_from_right)
        binding.relativeAnim1.startAnimation(relativeAnimRToL)
    }

    private fun onOptionSelect(
        partnerId: String?,
        isCorrect: String,
        choiceAnswer: String,
        marks: String,
        opponentTeamId: String?
    ) {
        firebaseDatabase.createShowAnimForAnotherUser(partnerId, isCorrect, choiceAnswer, marks)
        firebaseDatabase.createShowAnimForOpponentTeam(opponentTeamId ?: "", isCorrect,marks)
    }

    private fun getQuestions() {
        if (!CollectionUtils.isEmpty(question.que) && position < question.que.size) {
            questionSize = question.que.size
            val allQuestionsAnswer: Question = question.que[position]

            //animateGetReady(binding.roundNumber)
            binding.question.visibility = View.VISIBLE
            binding.question.text = allQuestionsAnswer.question
//            lifecycleScope.launch {
//                //delay(1000)
//                binding.question.visibility = View.VISIBLE
//                binding.question.text = allQuestionsAnswer.question
//            }
            answerAnim()
            choiceValue = getDisplayAnswerAfterQuesComplete()
            binding.answer1.text = allQuestionsAnswer.choices?.get(0)?.choiceData
            binding.answer2.text = allQuestionsAnswer.choices?.get(1)?.choiceData
            binding.answer3.text = allQuestionsAnswer.choices?.get(2)?.choiceData
            binding.answer4.text = allQuestionsAnswer.choices?.get(3)?.choiceData
        } else {
            openWinScreen()
        }
    }
    private fun setupViewModel() {
        questionRepo = QuestionRepo()
        factory = QuestionProviderFactory(requireActivity().application, questionRepo!!)
        questionViewModel = ViewModelProvider(this, factory!!).get(QuestionViewModel::class.java)
        questionViewModel = factory.let { ViewModelProvider(this, it!!).get(QuestionViewModel::class.java) }
        questionViewModel?.getQuizQuestion()
        try {
            // questionViewModel?.getRoomUserData(RoomData(roomId?:""))
            questionViewModel?.getRoomUserDataTemp(
                RandomRoomData(
                    roomId ?: "",
                    currentUserId ?: ""
                )
            )
        } catch (e: Exception) {

        }
    }
    private fun startTimer() {
        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var seconds = (millisUntilFinished / 1000).toInt()
                seconds %= 60
                binding.count.text = String.format("%2d", seconds)
                if (seconds == 5) {
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    activity?.application?.let {
                        AudioManagerQuiz.audioRecording.startPlaying(
                            it,
                            R.raw.voice_5_second
                        )
                    }
                }
            }

            override fun onFinish() {
                // getDisplay() walla api call yaha karna hai 10 sec bad walla
                displayAnswerAndShowNextQuestion()
            }
        }.start()
    }
    fun displayAnswerAndShowNextQuestion(){
        when (choiceValue) {
            binding.answer1.text -> {
                binding.card2.visibility = View.INVISIBLE
                binding.card3.visibility = View.INVISIBLE
                binding.card4.visibility = View.INVISIBLE
                binding.answer1.setTextColor(resources.getColor(R.color.green_quiz))
            }
            binding.answer2.text -> {
                binding.card1.visibility = View.INVISIBLE
                binding.card3.visibility = View.INVISIBLE
                binding.card4.visibility = View.INVISIBLE
                binding.answer2.setTextColor(resources.getColor(R.color.green_quiz))
            }
            binding.answer3.text -> {
                binding.card1.visibility = View.INVISIBLE
                binding.card2.visibility = View.INVISIBLE
                binding.card4.visibility = View.INVISIBLE
                binding.answer3.setTextColor(resources.getColor(R.color.green_quiz))
            }
            binding.answer4.text -> {
                binding.card1.visibility = View.INVISIBLE
                binding.card2.visibility = View.INVISIBLE
                binding.card3.visibility = View.INVISIBLE
                binding.answer4.setTextColor(resources.getColor(R.color.green_quiz))
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            binding.card1.visibility = View.INVISIBLE
            binding.card2.visibility = View.INVISIBLE
            binding.card3.visibility = View.INVISIBLE
            binding.card4.visibility = View.INVISIBLE

            binding.answer1.setTextColor(resources.getColor(R.color.black_quiz))
            binding.answer2.setTextColor(resources.getColor(R.color.black_quiz))
            binding.answer3.setTextColor(resources.getColor(R.color.black_quiz))
            binding.answer4.setTextColor(resources.getColor(R.color.black_quiz))
            position++
            if(position<questionSize.minus(1)){
                binding.roundNumber.visibility = View.VISIBLE
                binding.roundNumber.text = getString(R.string.round_1) + " " + position.plus(1).toString()
                animationForRound()
            }else{
                binding.roundNumber.visibility = View.INVISIBLE
            }
            binding.question.visibility = View.INVISIBLE
            //binding.roundNumber.visibility = View.VISIBLE
            binding.marks1.setTextColor(resources.getColor(R.color.white))
            binding.marks2.setTextColor(resources.getColor(R.color.white))
            makeAgainCardSquare(binding.card1)
            makeAgainCardSquare(binding.card2)
            makeAgainCardSquare(binding.card3)
            makeAgainCardSquare(binding.card4)
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                getQuestions()
            }
        }
    }
    fun showRoomUserData() {
        activity?.let {
            questionViewModel?.roomUserDataTemp?.observe(it, Observer {
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

            try {
                firebaseDatabase.getOpponentShowAnim(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getAnimShow(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getPartnerCutCard(currentUserTeamId?:"")
                firebaseDatabase.getOpponentCutCard(currentUserTeamId?:"")
            } catch (ex: Exception) {
                Log.d("error_fire", "initializeUsersTeamsData: " + ex.message)
            }

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

            currentUserTeamId = team2Id
            opponentTeamId = team1Id

            try {
                firebaseDatabase.getOpponentShowAnim(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getAnimShow(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getPartnerCutCard(currentUserTeamId?:"")
                firebaseDatabase.getOpponentCutCard(currentUserTeamId?:"")
            }catch (ex:Exception){
                Log.d("error_fire", "initializeUsersTeamsData: " + ex.message)
            }

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
    /* private fun showRoomUserData(){
        try {
            activity?.let {
                questionViewModel?.roomUserData?.observe(it, { roomData ->

                    teamId1 = roomData?.data?.get(0)?.teamId
                    teamId2 = roomData.data?.get(1)?.teamId

                    team1UserId1 = roomData.data?.get(0)?.userData?.get(0)?.userId
                    team1UserId2 = roomData.data?.get(0)?.userData?.get(1)?.userId
                    team2UserId1 = roomData.data?.get(1)?.userData?.get(0)?.userId
                    team2UserId2 = roomData.data?.get(1)?.userData?.get(1)?.userId


                    val team1UserName1 = roomData.data?.get(0)?.userData?.get(0)?.userName
                    val team1UserImageUrl1 = roomData?.data?.get(0)?.userData?.get(0)?.imageUrl

                    val team1UserName2 = roomData.data?.get(0)?.userData?.get(1)?.userName
                    val team1UserImageUrl2 = roomData?.data?.get(0)?.userData?.get(1)?.imageUrl

                    val team2UserName1 = roomData.data?.get(1)?.userData?.get(0)?.userName
                    val team2UserImageUrl1 = roomData.data?.get(1)?.userData?.get(0)?.imageUrl

                    val team2UserName2 = roomData.data?.get(1)?.userData?.get(1)?.userName
                    val team2UserImageUrl2 = roomData.data?.get(1)?.userData?.get(1)?.imageUrl

                    binding.team2User2Name.text = team2UserName2

                        //current user id yaha pass karna hai
                         if (team1UserId1 == currentUserId || team1UserId2 == currentUserId){
                             binding.team1User1Name.text = team1UserName1
                             ImageAdapter.imageUrl(
                                 binding.team1UserImage1,
                                 Mentor.getInstance().getUser()?.photo
                             )

                             binding.team1User2Name.text = team1UserName2
                             ImageAdapter.imageUrl(
                                 binding.team1UserImage2,
                                 team1UserImageUrl2
                             )

                             binding.team2User1Name.text = team2UserName1
                             ImageAdapter.imageUrl(
                                 binding.team2UserImage1,
                                 team2UserImageUrl1
                             )

                             binding.team2User2Name.text = team2UserName2
                             ImageAdapter.imageUrl(
                                 binding.team2UserImage2,
                                 team2UserImageUrl2
                             )
                         }
                          else if (team2UserId1 ==currentUserId || team2UserId2 == currentUserId) {
                             binding.team1User1Name.text = team2UserName1
                             ImageAdapter.imageUrl(
                                 binding.team1UserImage1,
                                 Mentor.getInstance().getUser()?.photo
                             )

                             binding.team1User2Name.text = team2UserName2
                             ImageAdapter.imageUrl(
                                 binding.team1UserImage2,
                                 team1UserImageUrl2
                             )

                             binding.team2User1Name.text = team1UserName1
                             ImageAdapter.imageUrl(
                                 binding.team2UserImage1,
                                 team1UserImageUrl1
                             )

                             binding.team2User2Name.text = team1UserName2
                             ImageAdapter.imageUrl(binding.team2UserImage2, team1UserImageUrl2)
                         }
                })
            }
        }catch (ex:Exception){

        }
    }*/
    private fun myDelay() {
        lifecycleScope.launch {
            binding.progress.animateProgress()
            binding.progress1.animateProgress()
        }
    }
    companion object {
        var marks: Int = 0
        var opponentTeamMarks :Int =0

        @JvmStatic
        fun newInstance(roomId: String?,startTime:String?) =
            QuestionFragment().apply {
                arguments = Bundle().apply {
                    putString("roomId", roomId)
                    putString("callTime",startTime)
                }
            }
    }
    private fun scaleAnimationForTeam1(v: View) {
        val animation = TranslateAnimation(20f, 20f, -200f, 20f)
        animation.duration = 1000
        animation.fillAfter = true
        // animation.setAnimationListener(MyAnimationListener())
        v.startAnimation(animation)
        v.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        progress1.visibility = View.VISIBLE

    }
    private fun scaleAnimationForTeam2(v: View) {
        val animation = TranslateAnimation(0f, 0f, -200f, 20f)
        animation.duration = 1000
        animation.fillAfter = true
        // animation.setAnimationListener(MyAnimationListener())
        v.startAnimation(animation)
        v.visibility = View.VISIBLE
    }
    private fun animationForText(v: View) {
        val animation1 = AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
        v.startAnimation(animation1)
        v.visibility = View.VISIBLE
    }
    private fun animationForRound() {
        val animation1 = AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
        binding.roundNumber.startAnimation(animation1)
        binding.layoutForRounds.visibility = View.VISIBLE

        animation1.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.layoutForRounds.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.layoutForRounds.visibility = View.INVISIBLE

            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

        })
    }
    private fun answerAnim() {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            binding.card1.visibility = View.VISIBLE
            binding.card2.visibility = View.VISIBLE
            binding.card3.visibility = View.VISIBLE
            binding.card4.visibility = View.VISIBLE

            val animation3 = AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
            binding.layoutCard.startAnimation(animation3)
        }, 2000)
        lifecycleScope.launch {
            delay(2000)
            myDelay()
            startTimer()
        }
    }
    private fun selectOptionCheck(
        teamUserId: String,
        teamId1: String?,
        teamId2: String?,
        roomId: String,
        pos: Int
    ) {
        //yaha ham current user ki mentor id se match karege agar vo uski hai tu uski team ki id nikelege
        if (teamUserId == team1UserId1 || teamUserId == team1UserId2) {
            givenTeamId = teamId1
        } else if (teamUserId == team2UserId1 || teamUserId == team2UserId2) {
            givenTeamId = teamId2
        }

        activity?.application?.let {
            AudioManagerQuiz.audioRecording.startPlaying(
                it,
                R.raw.user_click_anywhere
            )
        }
        questionViewModel?.getSelectOption(
            roomId,
            question.que[position].id ?: "",
            question.que[position].choices?.get(pos)?.id ?: "",
            givenTeamId ?: ""
        )
    }

    private fun getSelectOptionWithAnim(choiceAnswer: String, pos: Int) {
        disableCardClick()
        activity?.let {
            isCorrect = question.que[position].choices?.get(pos)?.isCorrect.toString()
            //isCorrect = "true"
            questionViewModel?.selectOption?.observe(it, {
                choiceAnswer(choiceAnswer, isCorrect ?: "")
                Log.d("both_team", "getSelectOptionWithAnim: "+it.message)
                if (it.message == "Both teams selected answer before 7 seconds , show response and animations to all the users in room") {

                    firebaseDatabase.createPartnerShowCutCard(currentUserTeamId?:"", isCorrect?:"", choiceAnswer)
                    firebaseDatabase.createOpponentTeamShowCutCard(opponentTeamId?:"", isCorrect?:"", choiceAnswer)

                    //hame yaha partner ke liye call back likhna hoga jis se ham usko card ke dono side cut dikha sake
                }
            })
            // partnerId = getPartnerId()

            if (isCorrect == "true") {
                marks += 10
            }
            binding.marks1.text = marks.toString()

            // yaha ham partner me uski id or answer bheje ge jis se usko vo answer dikha sake ki kya tick kiya hai
            isCorrect?.let { it1 ->
                onOptionSelect(
                    currentUserTeamId,
                    it1,
                    choiceAnswer,
                    marks.toString(),
                    opponentTeamId
                )
            }

        }
    }

    private fun getDisplayAnswerAfterQuesComplete(): String {
        questionViewModel?.getDisplayAnswerData(roomId ?: "", question.que[position].id ?: "")
        enableCardClick()
        activity?.let {
            questionViewModel?.displayAnswerData?.observe(it, { displayAnswerData ->
                choiceValue = displayAnswerData.correctChoiceValue
            })
        }
        return choiceValue ?: ""
    }

    //yaha ek array list hogi jisme sabhi user ki id hogi jisko hame animation dikhana hai
    // or apni team member ko answer bhi dikhana hai
    //or animation sabhi kio dikhnana

    private fun getPartnerId(): String? {
        var myPartnerId: String? = null
        if (team1UserId1 == currentUserId) {
            myPartnerId = team1UserId2
        } else if (team1UserId2 == currentUserId) {
            myPartnerId = team1UserId1
        } else if (team2UserId1 == currentUserId) {
            myPartnerId = team2UserId2
        } else if (team2UserId2 == currentUserId) {
            myPartnerId = team2UserId1
        }
        return myPartnerId
    }

    override fun onNotificationForInvitePartner(
        channelName: String,
        fromUserId: String,
        fromUserName: String,
        fromUserImage: String
    ) {
        TODO("Not yet implemented")
    }

    override fun onNotificationForPartnerNotAccept(
        userName: String?,
        userImageUrl: String,
        fromUserId: String
    ) {
        TODO("Not yet implemented")
    }

    override fun onNotificationForPartnerAccept(
        channelName: String?,
        timeStamp: String,
        isAccept: String,
        opponentMemberId: String,
        mentorId: String
    ) {
        TODO("Not yet implemented")
    }

    override fun onGetRoomId(currentUserRoomID: String?, mentorId: String) {

    }

    fun choiceAnswer(choiceAnswer: String, isCorrect: String) {
        disableCardClick()
        when (choiceAnswer) {
            binding.answer1.text -> {
                drawTriangleOnCard(binding.card1)
                binding.answer2.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer3.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer4.setTextColor(resources.getColor(R.color.black_quiz))

                if (isCorrect == "true") {
                    binding.marks1.setTextColor(resources.getColor(R.color.green_quiz))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.green_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer1.setTextColor(resources.getColor(R.color.green_quiz))
                } else {
                    binding.marks1.setTextColor(resources.getColor(R.color.red))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.red_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer1.setTextColor(resources.getColor(R.color.red))
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.answer1.setTextColor(resources.getColor(R.color.black_quiz))
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.black_line))
                }
            }
            binding.answer2.text -> {
                drawTriangleOnCard(binding.card2)
                binding.answer3.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer4.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer1.setTextColor(resources.getColor(R.color.black_quiz))

                if (isCorrect == "true") {
                    binding.marks1.setTextColor(resources.getColor(R.color.green_quiz))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.green_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer2.setTextColor(resources.getColor(R.color.green_quiz))
                } else {
                    binding.marks1.setTextColor(resources.getColor(R.color.red))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.red_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer2.setTextColor(resources.getColor(R.color.red))
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.answer2.setTextColor(resources.getColor(R.color.black_quiz))
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.black_line))
                }
            }
            binding.answer3.text -> {
                drawTriangleOnCard(binding.card3)
                binding.answer2.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer4.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer1.setTextColor(resources.getColor(R.color.black_quiz))

                if (isCorrect == "true") {
                    binding.marks1.setTextColor(resources.getColor(R.color.green_quiz))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.green_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer3.setTextColor(resources.getColor(R.color.green_quiz))
                } else {
                    binding.marks1.setTextColor(resources.getColor(R.color.red))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.red_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer3.setTextColor(resources.getColor(R.color.red))
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.answer3.setTextColor(resources.getColor(R.color.black_quiz))
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.black_line))
                }
            }
            binding.answer4.text -> {
                drawTriangleOnCard(binding.card4)
                binding.answer3.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer1.setTextColor(resources.getColor(R.color.black_quiz))
                binding.answer2.setTextColor(resources.getColor(R.color.black_quiz))

                if (isCorrect == "true") {
                    binding.marks1.setTextColor(resources.getColor(R.color.green_quiz))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.green_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer4.setTextColor(resources.getColor(R.color.green_quiz))
                } else {
                    binding.marks1.setTextColor(resources.getColor(R.color.red))
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.red_line))
                    binding.relativeAnim.startAnimation(animation)
                    binding.answer4.setTextColor(resources.getColor(R.color.red))
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.answer4.setTextColor(resources.getColor(R.color.black_quiz))
                    binding.relativeAnim.setBackgroundDrawable(resources.getDrawable(R.drawable.black_line))
                }
            }
        }
    }

    fun disableCardClick() {
        binding.card1.isClickable = false
        binding.card2.isClickable = false
        binding.card3.isClickable = false
        binding.card4.isClickable = false
    }
    fun enableCardClick() {
        binding.card1.isClickable = true
        binding.card2.isClickable = true
        binding.card3.isClickable = true
        binding.card4.isClickable = true
    }
    override fun onShowAnim(
        partnerUserId: String,
        isCorrect: String,
        choiceAnswer: String,
        marks1: String
    ) {
        //yaha animation show karna hai
        // val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
        // imageView1.startAnimation(animation)
        binding.progress1.pauseProgress()
        disableCardClick()
        choiceAnswer(choiceAnswer, isCorrect)
        marks = marks1.toInt()
        binding.marks1.text = marks.toString()
        firebaseDatabase.deleteAnimUser(partnerUserId)
    }

    override fun onOpponentShowAnim(opponentTeamId: String?, isCorrect: String,marksOpponentTeam: String) {
        opponentTeamMarks = marksOpponentTeam.toInt()
        binding.marks2.text = marksOpponentTeam
        binding.progress.pauseProgress()
        if (isCorrect == "true") {
            binding.marks2.setTextColor(resources.getColor(R.color.green_quiz))
            val animation = AnimationUtils.loadAnimation(context, R.anim.abc_popup_exit)
            binding.relativeAnim1.setBackgroundDrawable(resources.getDrawable(R.drawable.green_line))
            binding.relativeAnim1.startAnimation(animation)
        } else {
            binding.marks2.setTextColor(resources.getColor(R.color.red))
            val animation = AnimationUtils.loadAnimation(context, R.anim.abc_popup_exit)
            binding.relativeAnim1.setBackgroundDrawable(resources.getDrawable(R.drawable.red_line))
            binding.relativeAnim1.startAnimation(animation)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(500)
            binding.relativeAnim1.setBackgroundDrawable(resources.getDrawable(R.drawable.black_line))
        }

        // binding.marks1.text = marks.toString()
        firebaseDatabase.deleteOpponentAnimTeam(opponentTeamId ?: "")
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

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

//        yesBtn.setOnClickListener {
//            dialog.dismiss()
//            AudioManagerQuiz.audioRecording.stopPlaying()
//            openFavouritePartnerScreen()
//        }

        yesBtn.setOnClickListener {
            questionViewModel?.getClearRadius(RandomRoomData(roomId?:"",currentUserId?:""))
            activity?.let {
                questionViewModel?.clearRadius?.observe(it, {
                    Log.d("message", "showDialog: "+it.message)
                    dialog.dismiss()
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openFavouritePartnerScreen()
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
    fun openFavouritePartnerScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragnment.newInstance(), "Question"
            )
            ?.remove(this)
            ?.commit()
        fm?.popBackStack()
    }
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
    private fun openWinScreen() {
        val fm = activity?.supportFragmentManager
        val startTime :String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                WinScreenFragment.newInstance(
                    marks.toString(),
                    opponentTeamMarks.toString(),
                    roomId,
                    currentUserTeamId,
                    startTime
                ), "Win"
            )
            ?.commit()
    }
    override fun onStart() {
        super.onStart()
        binding.callTime.base = SystemClock.elapsedRealtime().minus(callTimeCount?.toLong()?:0)
        binding.callTime.start()
        Log.d("call_time_out", "onStart: "+callTimeCount)
    }

    override fun onOpponentPartnerCut(teamId: String, isCorrect: String, choiceAnswer: String) {
        when (choiceAnswer) {
            binding.answer1.text -> {
                drawTriangleOnCardRight(binding.card1)
                firebaseDatabase.deletePartnerCutCard(teamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer2.text -> {
                drawTriangleOnCardRight(binding.card2)
                firebaseDatabase.deletePartnerCutCard(teamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer3.text -> {
                drawTriangleOnCardRight(binding.card3)
                firebaseDatabase.deletePartnerCutCard(teamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer4.text -> {
                drawTriangleOnCardRight(binding.card4)
                firebaseDatabase.deletePartnerCutCard(teamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
        }
    }

    override fun onOpponentTeamCutCard(
        opponentTeamId: String,
        isCorrect: String,
        choiceAnswer: String
    ) {
        when (choiceAnswer) {
            binding.answer1.text -> {
                drawTriangleOnCardRight(binding.card1)
                firebaseDatabase.deleteOpponentCutCard(opponentTeamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer2.text -> {
                drawTriangleOnCardRight(binding.card2)
                firebaseDatabase.deleteOpponentCutCard(opponentTeamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer3.text -> {
                drawTriangleOnCardRight(binding.card3)
                firebaseDatabase.deleteOpponentCutCard(opponentTeamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
            binding.answer4.text -> {
                drawTriangleOnCardRight(binding.card4)
                firebaseDatabase.deleteOpponentCutCard(opponentTeamId)
//                timer?.cancel()
//                timer?.onFinish()
            }
        }
    }

}
