package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.*
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentQuestionBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionProviderFactory
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.QuestionViewModel
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.cell_grammar_heading_layout.*
import kotlinx.android.synthetic.main.fragment_both_team_mate_found.*
import kotlinx.android.synthetic.main.fragment_question.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestionFragment : Fragment(), FirebaseDatabase.OnNotificationTrigger,
    FirebaseDatabase.OnAnimationTrigger,
    P2pRtc.WebRtcEngineCallback {
    private lateinit var binding: FragmentQuestionBinding
    private var position: Int = 0
    private var questionSize: Int = 0
    private lateinit var question: QuestionResponse
    private var timer: CountDownTimer? = null
    private var givenTeamId: String? = null
    private var currentUserId: String? = null

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
    private var partnerId: String? = null

    private var callTimeCount: String? = null
    private var fromType: String? = null
    private var engine: RtcEngine? = null
    var progressStatus = 0
    var secondaryProgressStatus = 0
    private var flag = 1

    lateinit var progressBar: ProgressBar
    lateinit var progressBar1: ProgressBar

    val handlerForTimer = Handler(Looper.getMainLooper())
    var isUiShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        currentUserId = Mentor.getInstance().getId()
        arguments?.let {
            roomId = it.getString("roomId")
            callTimeCount = it.getString(CALL_TIME)
            fromType = it.getString(FROM_TYPE)
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

    init {
        position = 0
        marks = 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.container.setBackgroundColor(Color.WHITE)
        if (PrefManager.getBoolValue(USER_LEAVE_THE_GAME)) {
            showFadeImage()
        }
        if (PrefManager.getBoolValue(USER_MUTE_OR_NOT)) {
            muteUnMute(binding.team1Mic1)
        }
        Timber.d(currentUserId)
        progressBar = view.findViewById(R.id.vertical_progressbar)
        progressBar1 = view.findViewById(R.id.vertical_progressbar1)

        progressBar.max = 105
        progressBar1.max = 105
        scaleAnimationForTeam1(binding.imagesTeam1)
        scaleAnimationForTeam2(binding.imagesTeam2)
        animationForText(binding.txtLayout1)
        animationForText(binding.txtLayout2)
        slideAnimationLToR()
        slideAnimationRToL()
        showRoomUserData()
        onBackPress()
        AudioManagerQuiz.audioRecording.stopPlaying()
        if (position == 0) {
            animationForText(binding.roundNumber)
            lifecycleScope.launch(Dispatchers.Main) {
                myDelay()
                startTimer()
            }
        }

        activity?.let {
            try {
                questionViewModel?.questionData?.observe(it, Observer {
                    Timber.d(it.toString())
                    question = it
                    getQuestions()
                })
            } catch (ex: Exception) {
            }
        }
        try {
            engine = activity?.let { P2pRtc().initEngine(it) }
            P2pRtc().addListener(this)
        } catch (ex: Exception) {
        }

        binding.card1.setOnClickListener {
            try {
                AudioManagerQuiz.audioRecording.startPlaying(
                    requireActivity(),
                    R.raw.click,
                    false
                )
                drawTriangleOnCard(binding.imageCardLeft1)
                binding.progress1.pauseProgress()
                selectOptionCheck(
                    currentUserId ?: "",
                    team1Id,
                    team2Id,
                    roomId ?: "",
                    0
                )
                getSelectOptionWithAnim(binding.answer1.text.toString(), 0)
            } catch (ex: Exception) {
            }
        }
        binding.card2.setOnClickListener {
            try {
                AudioManagerQuiz.audioRecording.startPlaying(
                    requireActivity(),
                    R.raw.click,
                    false
                )
                drawTriangleOnCard(binding.imageCardLeft2)
                binding.progress1.pauseProgress()
                selectOptionCheck(
                    currentUserId ?: "",
                    team1Id,
                    team2Id,
                    roomId ?: "",
                    1
                )
                getSelectOptionWithAnim(binding.answer2.text.toString(), 1)
            } catch (ex: Exception) {
            }
        }
        binding.card3.setOnClickListener {
            try {
                AudioManagerQuiz.audioRecording.startPlaying(
                    requireActivity(),
                    R.raw.click,
                    false
                )
                drawTriangleOnCard(binding.imageCardLeft3)
                binding.progress1.pauseProgress()
                selectOptionCheck(
                    currentUserId ?: "",
                    team1Id,
                    team2Id,
                    roomId ?: "",
                    2
                )

                getSelectOptionWithAnim(binding.answer3.text.toString(), 2)
            } catch (ex: Exception) {
            }
        }
        binding.card4.setOnClickListener {
            try {
                AudioManagerQuiz.audioRecording.startPlaying(
                    requireActivity(),
                    R.raw.click,
                    false
                )
                drawTriangleOnCard(binding.imageCardLeft4)
                binding.progress1.pauseProgress()
                selectOptionCheck(
                    currentUserId ?: "",
                    team1Id,
                    team2Id,
                    roomId ?: "",
                    3
                )
                getSelectOptionWithAnim(binding.answer4.text.toString(), 3)
            } catch (ex: Exception) {
            }
        }

        try {
            binding.team1Mic1Click.setOnClickListener {
                muteUnMute(binding.team1Mic1)
            }
        } catch (ex: Exception) {
        }

        try {
            firebaseDatabase.getMuteOrUnMute(currentUserId ?: "", this)
        } catch (ex: Exception) {
        }
        buttonEnableDisable()
    }

    fun drawTriangleOnCard(view: View) {
        try {
            view.visibility = View.VISIBLE
        } catch (ex: Exception) {
        }
    }

    fun drawTriangleOnCardRight(view: View) {
        try {
            view.visibility = View.VISIBLE
        } catch (ex: Exception) {
        }
    }

    fun makeAgainCardSquare() {
        try {
            binding.imageCardLeft1.visibility = View.INVISIBLE
            binding.imageCardLeft2.visibility = View.INVISIBLE
            binding.imageCardLeft3.visibility = View.INVISIBLE
            binding.imageCardLeft4.visibility = View.INVISIBLE
            binding.imageCardRight1.visibility = View.INVISIBLE
            binding.imageCardRight2.visibility = View.INVISIBLE
            binding.imageCardRight3.visibility = View.INVISIBLE
            binding.imageCardRight4.visibility = View.INVISIBLE
        } catch (ex: Exception) {
        }
    }

    fun slideAnimationLToR() {
        val relativeAnimLToR = AnimationUtils.loadAnimation(context, R.anim.hs__slide_in_from_left)
        binding.verticalProgressbar.startAnimation(relativeAnimLToR)
    }

    fun slideAnimationRToL() {
        val relativeAnimRToL = AnimationUtils.loadAnimation(context, R.anim.hs__slide_in_from_right)
        binding.verticalProgressbar1.startAnimation(relativeAnimRToL)
    }

    private fun onOptionSelect(
        partnerId: String?,
        isCorrect: String,
        choiceAnswer: String,
        marks: String,
        opponentTeamId: String?
    ) {
        firebaseDatabase.createShowAnimForAnotherUser(partnerId, isCorrect, choiceAnswer, marks)
        firebaseDatabase.createShowAnimForOpponentTeam(opponentTeamId ?: "", isCorrect, marks)
    }

    private fun getQuestions() {
        if (!CollectionUtils.isEmpty(question.que) && position < question.que.size) {
            questionSize = question.que.size
            val allQuestionsAnswer: Question = question.que[position]

            binding.question.visibility = View.VISIBLE
            binding.question.text = allQuestionsAnswer.question?.trim()
            answerAnim()
            choiceValue = getDisplayAnswerAfterQuesComplete()
            binding.answer1.text = allQuestionsAnswer.choices?.get(0)?.choiceData?.trim()
            binding.answer2.text = allQuestionsAnswer.choices?.get(1)?.choiceData?.trim()
            binding.answer3.text = allQuestionsAnswer.choices?.get(2)?.choiceData?.trim()
            binding.answer4.text = allQuestionsAnswer.choices?.get(3)?.choiceData?.trim()
        } else {
            openWinScreen()
        }
    }

    private fun setupViewModel() {
        try {
            factory = QuestionProviderFactory(requireActivity().application)
            questionViewModel =
                ViewModelProvider(this, factory!!).get(QuestionViewModel::class.java)
            questionViewModel =
                factory.let { ViewModelProvider(this, it!!).get(QuestionViewModel::class.java) }
            questionViewModel?.getQuizQuestion(
                QuestionRequest(
                    QUESTION_COUNT,
                    roomId ?: "",
                    currentUserId
                )
            )
            try {
                questionViewModel?.getRoomUserDataTemp(
                    RandomRoomData(
                        roomId ?: "",
                        currentUserId ?: ""
                    )
                )
            } catch (e: Exception) {

            }
        } catch (ex: Exception) {

        }
    }

    private fun startTimer() {
        if (!AudioManagerQuiz.audioRecording.isPlaying()) {
            AudioManagerQuiz.audioRecording.startPlaying(
                requireContext(),
                R.raw.running_quiz_sound,
                false
            )
        }
        try {
            timer = object : CountDownTimer(16000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    seconds = (millisUntilFinished / 1000).toInt()
                    seconds %= 60
                    binding.count.text = String.format("%2d", seconds)
                    if (seconds == 5) {
                        activity?.application?.let {
                            AudioManagerQuiz.audioRecording.startPlaying(
                                it,
                                R.raw.voice_5_second,
                                false
                            )
                        }
                    }
                }

                override fun onFinish() {
                    Timber.d("$position")
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    displayAnswerAndShowNextQuestion()
                }
            }
            lifecycleScope.launch(Dispatchers.Main) {
                delay(2000)
                timer?.start()
            }
        } catch (ex: Exception) {
        }
    }

    fun displayAnswerAndShowNextQuestion() {
        try {
            when (choiceValue) {
                binding.answer1.text -> {
                    binding.card2.visibility = View.INVISIBLE
                    binding.card3.visibility = View.INVISIBLE
                    binding.card4.visibility = View.INVISIBLE

                    binding.answer1.setTextColor(getGreenColor())
                }
                binding.answer2.text -> {
                    binding.card1.visibility = View.INVISIBLE
                    binding.card3.visibility = View.INVISIBLE
                    binding.card4.visibility = View.INVISIBLE

                    binding.answer2.setTextColor(getGreenColor())
                }
                binding.answer3.text -> {
                    binding.card1.visibility = View.INVISIBLE
                    binding.card2.visibility = View.INVISIBLE
                    binding.card4.visibility = View.INVISIBLE

                    binding.answer3.setTextColor(getGreenColor())
                }
                binding.answer4.text -> {
                    binding.card1.visibility = View.INVISIBLE
                    binding.card2.visibility = View.INVISIBLE
                    binding.card3.visibility = View.INVISIBLE
                    binding.answer4.setTextColor(getGreenColor())
                }
            }
        } catch (ex: Exception) {
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            binding.card1.visibility = View.INVISIBLE
            binding.card2.visibility = View.INVISIBLE
            binding.card3.visibility = View.INVISIBLE
            binding.card4.visibility = View.INVISIBLE

            binding.answer1.setTextColor(getBlackColor())
            binding.answer2.setTextColor(getBlackColor())
            binding.answer3.setTextColor(getBlackColor())
            binding.answer4.setTextColor(getBlackColor())
            position++
            try {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(100)
                    progressBar.progress = marks
                    progressBar1.progress = opponentTeamMarks
                    progressBar.progressTintList = ColorStateList.valueOf(getBlue2Color())

                    progressBar1.progressTintList = ColorStateList.valueOf(getBlue2Color())
                }
            } catch (ex: Exception) {

            }
            try {
                if (position <= questionSize.minus(1)) {
                    if (position == questionSize.minus(1)) {
                        isLastQuestion = true
                        binding.roundNumber.visibility = View.VISIBLE
                        binding.roundNumber.text = LAST_ROUND
                        binding.getReady.visibility = View.VISIBLE
                        binding.getReady.text = ROUND_2X_BOUNCE
                        animationForRound()
                    } else {
                        binding.roundNumber.visibility = View.VISIBLE
                        binding.roundNumber.text =
                            "ROUND" + " " + position.plus(1).toString()

                        animationForRound()
                    }
                } else {
                    binding.roundNumber.visibility = View.INVISIBLE
                }
                binding.question.visibility = View.INVISIBLE
                binding.marks1.setTextColor(getWhiteColor())
                binding.marks2.setTextColor(getWhiteColor())
            } catch (ex: Exception) {
            }

            makeAgainCardSquare()
            firebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: "")
            firebaseDatabase.deletePartnerCutCard(currentUserTeamId ?: "")

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

            currentUserTeamId = team1Id
            opponentTeamId = team2Id

            partnerId = if (team1UserId1 == currentUserId) {
                team1UserId2
            } else {
                team1UserId1
            }

            try {
                firebaseDatabase.getOpponentShowAnim(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getAnimShow(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getPartnerCutCard(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getOpponentCutCard(currentUserTeamId ?: "", this@QuestionFragment)
            } catch (ex: Exception) {
                Timber.d(ex)
            }

            val imageUrl1 = team1User1ImageUrl?.replace("\n", "")
            binding.team1UserImage1.setUserImageOrInitials(
                imageUrl1,
                team1User1Name ?: "",
                30,
                true
            )
            binding.team1User1Name.text = getSplitName(team1User1Name)


            val imageUrl2 = team1User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = getSplitName(team1User2Name)

            val imageUrl3 = team2User1ImageUrl?.replace("\n", "")
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team2User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = getSplitName(team2User1Name)

            val imageUrl4 = team2User2ImageUrl?.replace("\n", "")
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = getSplitName(team2User2Name)

        } else if (team2UserId1 == currentUserId || team2UserId2 == currentUserId) {

            currentUserTeamId = team2Id
            opponentTeamId = team1Id

            partnerId = if (team2UserId1 == currentUserId) {
                team2UserId2
            } else {
                team2UserId1
            }

            try {
                firebaseDatabase.getOpponentShowAnim(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getAnimShow(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getPartnerCutCard(currentUserTeamId ?: "", this@QuestionFragment)
                firebaseDatabase.getOpponentCutCard(currentUserTeamId ?: "", this@QuestionFragment)
            } catch (ex: Exception) {
                Timber.d(ex)
            }

            val imageUrl1 = team2User1ImageUrl?.replace("\n", "")
            binding.team1UserImage1.setUserImageOrInitials(
                imageUrl1,
                team2User1Name ?: "",
                30,
                true
            )
            binding.team1User1Name.text = getSplitName(team2User1Name)

            val imageUrl2 = team2User2ImageUrl?.replace("\n", "")
            binding.team1UserImage2.setUserImageOrInitials(
                imageUrl2,
                team2User2Name ?: "",
                30,
                true
            )
            binding.team1User2Name.text = getSplitName(team2User2Name)

            val imageUrl3 = team1User1ImageUrl?.replace("\n", "")
            binding.team2UserImage1.setUserImageOrInitials(
                imageUrl3,
                team1User1Name ?: "",
                30,
                true
            )
            binding.team2User1Name.text = getSplitName(team1User1Name)

            val imageUrl4 = team1User2ImageUrl?.replace("\n", "")
            binding.team2UserImage2.setUserImageOrInitials(
                imageUrl4,
                team1User2Name ?: "",
                30,
                true
            )
            binding.team2User2Name.text = getSplitName(team1User2Name)

        }
    }

    private fun myDelay() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progress.animateProgress()
            binding.progress1.animateProgress()
        }
    }

    companion object {
        var marks: Int = 0
        var opponentTeamMarks: Int = 0
        var isLastQuestion = false
        var seconds = 0

        @JvmStatic
        fun newInstance(roomId: String?, startTime: String?, fromType: String?) =
            QuestionFragment().apply {
                arguments = Bundle().apply {
                    putString("roomId", roomId)
                    putString(CALL_TIME, startTime)
                    putString(FROM_TYPE, fromType)
                }
            }
    }

    private fun scaleAnimationForTeam1(v: View) {
        try {
            val animation = TranslateAnimation(20f, 20f, -200f, 20f)
            animation.duration = 1000
            animation.fillAfter = true
            v.startAnimation(animation)
            v.visibility = View.VISIBLE
            progress.visibility = View.VISIBLE
            progress1.visibility = View.VISIBLE
        } catch (ex: Exception) {
        }
    }

    private fun scaleAnimationForTeam2(v: View) {
        try {
            val animation = TranslateAnimation(0f, 0f, -200f, 20f)
            animation.duration = 1000
            animation.fillAfter = true
            v.startAnimation(animation)
            v.visibility = View.VISIBLE
        } catch (ex: Exception) {
        }
    }

    private fun animationForText(v: View) {
        try {
            val animation1 = AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
            v.startAnimation(animation1)
            v.visibility = View.VISIBLE
        } catch (ex: Exception) {
        }
    }

    private fun animationForRound() {
        try {
            val animation1 = AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
            binding.roundNumber.startAnimation(animation1)
            binding.layoutForRounds.visibility = View.VISIBLE
            binding.count.text = activity?.resources?.getText(R.string.fiften)
            binding.progress.setAnimZero()
            binding.progress1.setAnimZero()

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

            lifecycleScope.launch(Dispatchers.Main) {
                myDelay()
                startTimer()
            }
        } catch (ex: Exception) {
        }
    }

    private fun answerAnim() {
        try {
            if (isUiShown) {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(1000)
                    binding.card1.visibility = View.VISIBLE
                    binding.card2.visibility = View.VISIBLE
                    binding.card3.visibility = View.VISIBLE
                    binding.card4.visibility = View.VISIBLE

                    val animation3 =
                        AnimationUtils.loadAnimation(activity, R.anim.fade_out_for_text)
                    binding.layoutCard.startAnimation(animation3)
                }
            }
        } catch (ex: Exception) {
        }
    }

    private fun selectOptionCheck(
        teamUserId: String,
        teamId1: String?,
        teamId2: String?,
        roomId: String,
        pos: Int
    ) {
        if (teamUserId == team1UserId1 || teamUserId == team1UserId2) {
            givenTeamId = teamId1
        } else if (teamUserId == team2UserId1 || teamUserId == team2UserId2) {
            givenTeamId = teamId2
        }

        try {
            activity?.application?.let {
                AudioManagerQuiz.audioRecording.startPlaying(
                    it,
                    R.raw.click,
                    false
                )
            }

            questionViewModel?.getSelectOption(
                roomId,
                question.que[position].id ?: "",
                question.que[position].choices?.get(pos)?.id ?: "",
                givenTeamId ?: ""
            )
        } catch (ex: Exception) {

        }
    }

    private fun getSelectOptionWithAnim(choiceAnswer: String, pos: Int) {
        disableCardClick()
        try {

            activity?.let {
                isCorrect = question.que[position].choices?.get(pos)?.isCorrect.toString()
                questionViewModel?.selectOption?.observe(it, {
                    choiceAnswer(choiceAnswer, isCorrect ?: "")
                    val firstTeamAnswer = it.choiceData?.get(0)?.choiceData
                    if (it.message == BOTH_TEAM_SELECTED) {
                        firebaseDatabase.createOpponentTeamShowCutCard(
                            opponentTeamId ?: "",
                            isCorrect ?: "",
                            choiceAnswer
                        )
                        firebaseDatabase.createPartnerShowCutCard(
                            currentUserTeamId ?: "",
                            isCorrect ?: "",
                            firstTeamAnswer ?: ""
                        )
                    }
                })
                if (isCorrect == TRUE) {
                    marks += if (isLastQuestion) {
                        (seconds * 2)
                    } else {
                        seconds
                    }
                }

                try {
                    if (isCorrect == TRUE) {
                        if (position != 0)
                            progressBar.progressTintList = ColorStateList.valueOf(getGreenColor())

                        binding.marks1.text = marks.toString()
                        secondaryProgressStatus = marks
                        progressBar.secondaryProgress = secondaryProgressStatus

                        progressBar.progress = marks - (seconds)
                    } else {
                        progressBar.progress = marks
                        progressBar.progressTintList = ColorStateList.valueOf(getBlue2Color())
                    }
                } catch (ex: Exception) {

                }
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
        } catch (ex: Exception) {
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

    override fun onGetRoomId(currentUserRoomID: String?, mentorId: String) {

    }

    fun choiceAnswer(choiceAnswer: String, isCorrect: String) {
        disableCardClick()
        when (choiceAnswer) {
            binding.answer1.text -> {
                try {
                    drawTriangleOnCard(binding.imageCardLeft1)
                    binding.answer2.setTextColor(getBlackColor())
                    binding.answer3.setTextColor(getBlackColor())
                    binding.answer4.setTextColor(getBlackColor())

                    if (isCorrect == TRUE) {
                        binding.marks1.setTextColor(getGreenColor())
                        binding.answer1.setTextColor(getGreenColor())
                    } else {
                        binding.marks1.setTextColor(getRedColor())
                        binding.answer1.setTextColor(getRedColor())
                    }
                } catch (ex: Exception) {
                }
            }
            binding.answer2.text -> {
                try {
                    drawTriangleOnCard(binding.imageCardLeft2)
                    binding.answer3.setTextColor(getBlackColor())
                    binding.answer4.setTextColor(getBlackColor())
                    binding.answer1.setTextColor(getBlackColor())
                    if (isCorrect == TRUE) {
                        binding.marks1.setTextColor(getGreenColor())
                        binding.answer2.setTextColor(getGreenColor())
                    } else {
                        binding.marks1.setTextColor(getRedColor())
                        binding.answer2.setTextColor(getRedColor())
                    }
                } catch (ex: Exception) {
                }
            }
            binding.answer3.text -> {
                try {
                    drawTriangleOnCard(binding.imageCardLeft3)
                    binding.answer2.setTextColor(getBlackColor())
                    binding.answer4.setTextColor(getBlackColor())
                    binding.answer1.setTextColor(getBlackColor())

                    if (isCorrect == TRUE) {
                        binding.marks1.setTextColor(getGreenColor())
                        binding.answer3.setTextColor(getGreenColor())
                    } else {
                        binding.marks1.setTextColor(getRedColor())
                        binding.answer3.setTextColor(getRedColor())
                    }
                } catch (ex: Exception) {
                }
            }
            binding.answer4.text -> {
                try {
                    drawTriangleOnCard(binding.imageCardLeft4)
                    binding.answer3.setTextColor(getBlackColor())
                    binding.answer1.setTextColor(getBlackColor())
                    binding.answer2.setTextColor(getBlackColor())

                    if (isCorrect == TRUE) {
                        binding.marks1.setTextColor(getGreenColor())
                        binding.answer4.setTextColor(getGreenColor())
                    } else {
                        binding.marks1.setTextColor(getRedColor())
                        binding.answer4.setTextColor(getRedColor())

                    }
                } catch (ex: Exception) {
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
        binding.progress1.pauseProgress()
        disableCardClick()
        choiceAnswer(choiceAnswer, isCorrect)
        marks = marks1.toInt()
        binding.marks1.text = marks.toString()

        try {
            if (isCorrect == TRUE) {
                try {
                    if (isUiShown)
                        AudioManagerQuiz().rightAnswerPlaying(requireContext())
                    progressBar.secondaryProgress = marks
                    progressBar.progress = marks - 10
                    if (position != 0)
                        progressBar.progressTintList = ColorStateList.valueOf(getGreenColor())

                } catch (ex: Exception) {
                }
            } else {
                try {
                    if (isUiShown)
                        AudioManagerQuiz().wrongAnswerPlaying(requireContext())
                    progressBar.progress = marks
                    progressBar.progressTintList = ColorStateList.valueOf(getBlue2Color())
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                    ContextCompat.getDrawable(
                        AppObjectController.joshApplication,
                        R.drawable.vertical_red
                    )
                    binding.verticalProgressbar.setBackgroundDrawable(getVerticalRedDrawable())
                    binding.verticalProgressbar.startAnimation(animation)

                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(500)
                        binding.verticalProgressbar.setBackgroundDrawable(getVerticalBlackDrawable())
                    }
                } catch (ex: Exception) {
                }
            }
        } catch (ex: Exception) {
        }
        firebaseDatabase.deleteAnimUser(partnerUserId)
    }

    override fun onOpponentShowAnim(
        opponentTeamId: String?,
        isCorrect: String,
        marksOpponentTeam: String
    ) {
        opponentTeamMarks = marksOpponentTeam.toInt()
        binding.marks2.text = marksOpponentTeam
        binding.progress.pauseProgress()
        if (isCorrect == TRUE) {
            try {
                if (isUiShown)
                    AudioManagerQuiz().rightAnswerPlaying(requireContext())
                binding.marks2.setTextColor(getGreenColor())
                progressBar1.secondaryProgress = opponentTeamMarks
                progressBar1.progress = opponentTeamMarks - 10
                if (position != 0)
                    progressBar1.progressTintList = ColorStateList.valueOf(getGreenColor())

                val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                binding.verticalProgressbar1.setBackgroundDrawable(getVerticalGreenDrawable())
                binding.verticalProgressbar1.startAnimation(animation)

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.verticalProgressbar1.setBackgroundDrawable(getVerticalBlackDrawable())
                }
            } catch (ex: Exception) {
            }
        } else {
            try {
                if (isUiShown)
                    AudioManagerQuiz().wrongAnswerPlaying(requireContext())
                binding.marks2.setTextColor(getRedColor())
                progressBar1.progress = opponentTeamMarks
                progressBar1.progressTintList = ColorStateList.valueOf(getBlue2Color())
                val animation = AnimationUtils.loadAnimation(activity, R.anim.abc_popup_exit)
                binding.verticalProgressbar1.setBackgroundDrawable(getVerticalRedDrawable())
                binding.verticalProgressbar1.startAnimation(animation)

                lifecycleScope.launch(Dispatchers.Main) {
                    delay(500)
                    binding.verticalProgressbar1.setBackgroundDrawable(getVerticalBlackDrawable())
                }
            } catch (ex: Exception) {
            }
        }
        firebaseDatabase.deleteOpponentAnimTeam(opponentTeamId ?: "")
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
        questionViewModel?.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: "",
                startTime.toInt().div(1000).toString(),
                currentUserId ?: ""
            )
        )

        if (fromType == RANDOM) {
            try {
                questionViewModel?.getClearRadius(
                    SaveCallDurationRoomData(
                        roomId ?: "",
                        currentUserId ?: "",
                        currentUserTeamId ?: "",
                        callTimeCount ?: ""
                    )
                )
                activity?.let {
                    questionViewModel?.clearRadius?.observe(it, {
                        if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_AND_RADIUS) {
                            activity?.let {
                                questionViewModel?.saveCallDuration?.observe(it, {
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
            } catch (ex: Exception) {
            }
        } else {
            try {
                questionViewModel?.deleteUserRoomData(
                    SaveCallDurationRoomData(
                        roomId ?: "",
                        currentUserId ?: "",
                        currentUserTeamId ?: "",
                        callTimeCount ?: ""
                    )
                )
                activity?.let {
                    questionViewModel?.deleteData?.observe(it, {
                        if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_FPP) {
                            activity?.let {
                                questionViewModel?.saveCallDuration?.observe(it, {
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
            } catch (ex: Exception) {
            }
        }
    }

    private fun openFavouritePartnerScreen() {
        position = 0
        marks = 0
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "Question"
            )
            ?.remove(this)
            ?.commit()
        isLastQuestion = false
        marks = 0
        position = 0
        seconds = 0
        secondaryProgressStatus = 0
        progressStatus = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isUiShown = false
            PrefManager.put(USER_MUTE_OR_NOT, false)
            handlerForTimer.removeCallbacksAndMessages(null)
            timer?.cancel()
        } catch (ex: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        isUiShown = true
    }

    private fun openWinScreen() {
        val fm = activity?.supportFragmentManager
        val startTime: String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                WinScreenFragment.newInstance(
                    marks.toString(),
                    opponentTeamMarks.toString(),
                    roomId,
                    currentUserTeamId,
                    startTime,
                    fromType ?: ""
                ), "Win"
            )
            ?.remove(this)
            ?.commit()
        position = 0
        marks = 0
        isLastQuestion = false
        seconds = 0
        secondaryProgressStatus = 0
        progressStatus = 0
    }

    override fun onStart() {
        super.onStart()
        binding.callTime.base = SystemClock.elapsedRealtime().minus(callTimeCount?.toLong() ?: 0)
        binding.callTime.start()
    }

    override fun onOpponentPartnerCut(teamId: String, isCorrect: String, choiceAnswer: String) {
        try {
            when (choiceAnswer) {
                binding.answer1.text -> {
                    drawTriangleOnCard(binding.imageCardRight1)
                    firebaseDatabase.deletePartnerCutCard(teamId)
                }
                binding.answer2.text -> {
                    drawTriangleOnCard(binding.imageCardRight2)
                    firebaseDatabase.deletePartnerCutCard(teamId)
                }
                binding.answer3.text -> {
                    drawTriangleOnCard(binding.imageCardRight3)
                    firebaseDatabase.deletePartnerCutCard(teamId)
                }
                binding.answer4.text -> {
                    drawTriangleOnCard(binding.imageCardRight4)
                    firebaseDatabase.deletePartnerCutCard(teamId)
                }
            }
        } catch (ex: Exception) {
            //Timber.d(ex)
        }
    }

    override fun onOpponentTeamCutCard(
        opponentTeamId: String,
        isCorrect: String,
        choiceAnswer: String
    ) {
        try {
            when (choiceAnswer) {
                binding.answer1.text -> {
                    drawTriangleOnCardRight(binding.imageCardRight1)
                    firebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: "")
                }
                binding.answer2.text -> {
                    drawTriangleOnCardRight(binding.imageCardRight2)
                    firebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: "")
                }
                binding.answer3.text -> {
                    drawTriangleOnCardRight(binding.imageCardRight3)
                    firebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: "")
                }
                binding.answer4.text -> {
                    drawTriangleOnCardRight(binding.imageCardRight4)
                    firebaseDatabase.deleteOpponentCutCard(currentUserTeamId ?: "")
                }
            }
        } catch (ex: Exception) {
            //Timber.d(ex)
        }
    }

    override fun onMicOnOff(partnerUserId: String, status: String) {
        if (status == TRUE) {
            binding.team1Mic2.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_new_mic_off
                )
            )
        } else {
            binding.team1Mic2.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_new_mic
                )
            )
        }
    }

    override fun onPartnerLeave() {
        super.onPartnerLeave()
        PrefManager.put(USER_LEAVE_THE_GAME, true)
        binding.callTime.stop()
        showFadeImage()
    }

    private fun showFadeImage() {
        when {
            team1UserId1 == currentUserId -> {
                try {
                    requireActivity().runOnUiThread {
                        binding.team1User2Name.alpha = 0.5f
                        binding.team1UserImage2Shadow.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Timber.d(ex)
                }
            }
            team1UserId2 == currentUserId -> {
                try {
                    requireActivity().runOnUiThread {
                        binding.team1User1Name.alpha = 0.5f
                        binding.team1UserImage1Shadow.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Timber.d(ex)
                }
            }
            team2UserId1 == currentUserId -> {
                try {
                    requireActivity().runOnUiThread {
                        binding.team2User2Name.alpha = 0.5f
                        binding.team2UserImage2Shadow.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Timber.d(ex)
                }
            }
            team2UserId2 == currentUserId -> {
                try {
                    requireActivity().runOnUiThread {
                        binding.team2User1Name.alpha = 0.5f
                        binding.team2UserImage1Shadow.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Timber.d(ex)
                }
            }
        }
    }

    private fun muteCall() {
        engine?.muteLocalAudioStream(true)
    }

    private fun unMuteCall() {
        engine?.muteLocalAudioStream(false)
    }

    private fun muteUnMute(view: ImageView) {
        try {
            if (flag == 1) {
                flag = 0
                muteCall()
                view.setImageDrawable(
                    ContextCompat.getDrawable(
                        AppObjectController.joshApplication,
                        R.drawable.ic_new_mic_off
                    )
                )
                firebaseDatabase.createMicOnOff(partnerId ?: "", "true")
            } else {
                flag = 1
                unMuteCall()
                PrefManager.put(USER_MUTE_OR_NOT, false)
                view.setImageDrawable(
                    ContextCompat.getDrawable(
                        AppObjectController.joshApplication,
                        R.drawable.ic_new_mic
                    )
                )
                firebaseDatabase.createMicOnOff(partnerId ?: "", "false")
            }
        } catch (ex: Exception) {

        }
    }

    fun buttonEnableDisable() {
        binding.team1Mic1.isEnabled = true
        binding.team1Mic2.isEnabled = false
    }

    fun getGreenColor(): Int {
        return ContextCompat.getColor(AppObjectController.joshApplication, R.color.green_quiz)
    }

    fun getBlue2Color(): Int {
        return ContextCompat.getColor(AppObjectController.joshApplication, R.color.blue2)
    }

    fun getRedColor(): Int {
        return ContextCompat.getColor(AppObjectController.joshApplication, R.color.red)
    }

    fun getBlackColor(): Int {
        return ContextCompat.getColor(AppObjectController.joshApplication, R.color.black_quiz)
    }

    fun getWhiteColor(): Int {
        return ContextCompat.getColor(AppObjectController.joshApplication, R.color.white)
    }

    fun getVerticalRedDrawable(): Drawable? {
        return ContextCompat.getDrawable(
            AppObjectController.joshApplication,
            R.drawable.vertical_red
        )
    }

    fun getVerticalGreenDrawable(): Drawable? {
        return ContextCompat.getDrawable(
            AppObjectController.joshApplication,
            R.drawable.vertical_green
        )
    }

    fun getVerticalBlackDrawable(): Drawable? {
        return ContextCompat.getDrawable(
            AppObjectController.joshApplication,
            R.drawable.vertical_black
        )
    }

    fun getSplitName(name: String?): String {
        return name?.split(" ")?.get(0).toString()
    }
}
