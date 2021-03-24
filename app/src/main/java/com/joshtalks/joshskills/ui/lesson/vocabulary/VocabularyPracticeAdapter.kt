package com.joshtalks.joshskills.ui.lesson.vocabulary

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.extension.setImageAndFitCenter
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.PracticeItemLayoutBinding
import com.joshtalks.joshskills.databinding.VocabQuizPracticeItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import timber.log.Timber

const val PAUSE_AUDIO = "PAUSE_AUDIO"

class VocabularyPracticeAdapter(
    val context: Context,
    val itemList: List<LessonQuestion>,
    val assessmentQuizList: ArrayList<AssessmentWithRelations>,
    val clickListener: PracticeClickListeners,
    private var lifecycleProvider: LifecycleOwner,
    private val conversationId: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var audioManager = ExoAudioPlayer.getInstance()
    var currentQuestion: LessonQuestion? = null
    var currentPlayingPosition: Int = 0
    private var expandCardPosition: Int = -1
    private var QUIZ_TYPE: Int = 1
    private var VOCAB_TYPE: Int = 0
    var wordsItemSize = itemList.size.minus(assessmentQuizList.size)
    var revisionItemSize = assessmentQuizList.size
    val appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
        .addBasicParam()
        .addUserDetails()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VOCAB_TYPE -> {
                val binding = PracticeItemLayoutBinding.inflate(
                    LayoutInflater.from(context), parent, false
                ).apply {
                    lifecycleOwner = lifecycleProvider
                }
                return VocabularyViewHolder(binding)
            }
            else -> {
                val binding = VocabQuizPracticeItemLayoutBinding.inflate(
                    LayoutInflater.from(context), parent, false
                ).apply {
                    lifecycleOwner = lifecycleProvider
                }
                return QuizViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VOCAB_TYPE -> {
                (holder as VocabularyViewHolder).lessonQuestion = itemList[position]
                holder.positionInList = position
                holder.bind(position)
            }
            else -> {
                (holder as QuizViewHolder).lessonQuestion = itemList[position]
                holder.positionInList = position
                assessmentQuizList.filter { it.assessment.remoteId == itemList[position].assessmentId }
                    .getOrNull(0)?.let { assessmentWithRelations ->
                        holder.assessmentWithRelations = assessmentWithRelations
                        holder.bind(position)
                    }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] as String == PAUSE_AUDIO) {
            (holder as VocabularyViewHolder).binding.root.tag = itemList[position].id
            holder.lessonQuestion = itemList[position]
            holder.positionInList = position
            holder.pauseAudio()
        } else {
            when (holder.itemViewType) {
                VOCAB_TYPE -> {
                    (holder as VocabularyViewHolder).lessonQuestion = itemList[position]
                    holder.positionInList = position
                    holder.bind(position)
                }
                else -> {
                    (holder as QuizViewHolder).lessonQuestion = itemList[position]
                    holder.positionInList = position
                    assessmentQuizList.filter { it.assessment.remoteId == itemList[position].assessmentId }
                        .getOrNull(0)?.let { assessmentWithRelations ->
                            holder.assessmentWithRelations = assessmentWithRelations
                            holder.bind(position)
                        }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position].type) {
            LessonQuestionType.QUIZ -> {
                QUIZ_TYPE
            }
            else -> {
                VOCAB_TYPE
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return itemList[position].id.toLong()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is VocabularyViewHolder) {
            holder.complete()
        }
    }

    fun updateAssessmentQuizList(assessmentQuizList: ArrayList<AssessmentWithRelations>) {
        this.assessmentQuizList.clear()
        this.assessmentQuizList.addAll(assessmentQuizList)
        this.wordsItemSize = itemList.size.minus(assessmentQuizList.size)
        this.revisionItemSize = assessmentQuizList.size
        notifyDataSetChanged()
    }

    inner class QuizViewHolder(
        val binding: VocabQuizPracticeItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var isCorrect: Boolean = false
        var lessonQuestion: LessonQuestion? = null
        var positionInList = -1
        var assessmentWithRelations: AssessmentWithRelations? = null
        private val quizCheckedChangeListener =
            RadioGroup.OnCheckedChangeListener { radioGroup: RadioGroup, checkedId: Int ->
                resetAllRadioButtonsBg()
                binding.submitAnswerBtn.isEnabled = true
                radioGroup.findViewById<RadioButton>(checkedId)?.setBackgroundColor(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.received_bg_BC
                    )
                )
            }

        init {
            initViewHolder()
        }

        private fun initViewHolder() {
            binding.handler = this
            binding.quizRadioGroup.setOnCheckedChangeListener(quizCheckedChangeListener)
            binding.practiceTitleTv.setOnClickListener {
                if (binding.quizLayout.visibility == GONE) {
                    expandCard()
                } else {
                    collapseCard()
                }
            }
            binding.expandIv.setOnClickListener {
                if (binding.quizLayout.visibility == GONE) {
                    expandCard()
                } else {
                    collapseCard()
                }
            }
            binding.submitAnswerBtn.setOnClickListener {
                if (lessonQuestion != null && assessmentWithRelations != null) {
                    onSubmitQuizClick(lessonQuestion!!, assessmentWithRelations!!.questionList[0])
                }
            }
            binding.showExplanationBtn.setOnClickListener {
                showExplanation()
            }
            binding.continueBtn.setOnClickListener {
                if (lessonQuestion != null && assessmentWithRelations != null) {
                    binding.continueBtn.visibility = GONE
                    expandCardPosition = positionInList + 1
                    val hasNextItem = positionInList < itemList.size - 1
                    clickListener.submitQuiz(
                        lessonQuestion!!,
                        isCorrect,
                        assessmentWithRelations!!.questionList[0].question.remoteId,
                        positionInList,
                        hasNextItem,
                        true
                    )
                }
            }
        }

        fun bind(
            position: Int
        ) {
            if (assessmentWithRelations?.questionList?.getOrNull(0) == null) {
                return
            }
            if (expandCardPosition == positionInList && lessonQuestion?.status == QUESTION_STATUS.NA) {
                expandCardPosition = -1
                expandCard()
                if (position > 0)
                    clickListener.focusChild(position - 1)
            } else {
                collapseCard()
            }

            val assessmentQuestion = assessmentWithRelations!!.questionList[0]
            isCorrect = assessmentQuestion.question.status == QuestionStatus.CORRECT
            updateRadioGroupUI(assessmentQuestion)
            val revisionNumber =
                itemList.filter { it.assessmentId != null }.indexOf(lessonQuestion) + 1
            binding.practiceTitleTv.text =
                AppObjectController.joshApplication.getString(
                    R.string.quiz_tag,
                    revisionNumber,
                    revisionItemSize
                )

            if (lessonQuestion?.status == QUESTION_STATUS.IP) {
                binding.quizLayout.visibility = VISIBLE
                binding.continueBtn.visibility = VISIBLE
                assessmentQuestion.reviseConcept?.let {
                    binding.showExplanationBtn.visibility = VISIBLE
                }
                binding.submitAnswerBtn.visibility = GONE
                expandCard()
                if (position > 0)
                    clickListener.focusChild(position - 1)
            }
        }

        private fun onSubmitQuizClick(
            lessonQuestion: LessonQuestion,
            assessmentQuestion: AssessmentQuestionWithRelations
        ) {
            //binding.quizRadioGroup.tag is id of Radio button with correct answer.
            if (binding.quizRadioGroup.tag is Int) {
                assessmentQuestion.question.isAttempted = true
                assessmentQuestion.question.status =
                    evaluateQuestionStatus((binding.quizRadioGroup.tag as Int) == binding.quizRadioGroup.checkedRadioButtonId)
                isCorrect = assessmentQuestion.question.status == QuestionStatus.CORRECT

                val selectedChoice =
                    assessmentQuestion.choiceList[binding.quizRadioGroup.indexOfChild(
                        binding.root.findViewById(binding.quizRadioGroup.checkedRadioButtonId)
                    )]
                selectedChoice.isSelectedByUser = true
                selectedChoice.userSelectedOrder = 1

                //This will get Radio button of correct answer and set its background.
                binding.quizRadioGroup.findViewById<RadioButton>(binding.quizRadioGroup.tag as Int)
                    .setBackgroundResource(R.drawable.rb_correct_rect_bg)

                updateRadioGroupUI(assessmentQuestion)

                val hasNextItem = positionInList < itemList.size - 1
                val canShowSectionCompletedCard = false
                clickListener.submitQuiz(
                    lessonQuestion,
                    isCorrect,
                    assessmentQuestion.question.remoteId,
                    positionInList,
                    hasNextItem,
                    canShowSectionCompletedCard
                )

                clickListener.quizOptionSelected(
                    lessonQuestion,
                    assessmentQuestion,
                    positionInList,
                    hasNextItem
                )

                assessmentQuestion.reviseConcept?.let {
                    binding.showExplanationBtn.visibility = VISIBLE
                }

                binding.continueBtn.visibility = VISIBLE
                requestFocus(binding.showExplanationBtn)
            }
        }

        private fun expandCard() {
            binding.quizLayout.visibility = VISIBLE
            binding.expandIv.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_remove
                )
            )
        }

        private fun collapseCard() {
            binding.quizLayout.visibility = GONE
            binding.expandIv.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_add
                )
            )
        }

        fun hideExplanation() {
            binding.explanationLbl.visibility = GONE
            binding.explanationTv.visibility = GONE
            binding.showExplanationBtn.text =
                AppObjectController.joshApplication.getString(R.string.show_explanation)
        }

        fun showExplanation() {
            if (binding.explanationLbl.visibility == VISIBLE) {
                binding.showExplanationBtn.text =
                    AppObjectController.joshApplication.getString(R.string.show_explanation)
                binding.explanationLbl.visibility = GONE
                binding.explanationTv.visibility = GONE
            } else {
                binding.showExplanationBtn.text =
                    AppObjectController.joshApplication.getString(R.string.hide_explanation)
                binding.explanationLbl.visibility = VISIBLE
                binding.explanationTv.visibility = VISIBLE
                binding.explanationTv.requestFocus()
                requestFocus(binding.explanationTv)
            }
        }

        private fun requestFocus(view: View) {
            view.parent.requestChildFocus(
                view,
                view
            )
        }

        private fun evaluateQuestionStatus(status: Boolean): QuestionStatus {
            return if (status) QuestionStatus.CORRECT
            else QuestionStatus.WRONG

        }

        private fun updateRadioGroupUI(question: AssessmentQuestionWithRelations) {
            binding.quizQuestionTv.text = question.question.text
            hideExplanation()
            binding.explanationTv.text = question.reviseConcept?.description
            binding.quizRadioGroup.check(-1)
            var correctAns = false
            var setUpAnswerKey = false
            question.choiceList.forEachIndexed { index, choice ->
                when (index) {
                    0 -> {
                        //setupOption returns true if this option is correct answer of the question else false
                        setUpAnswerKey = setupRadioButtonOption(binding.option1, choice, question)
                        //correctAns.not() means correct answer is not found yet.
                        if (correctAns.not()) {
                            correctAns = setUpAnswerKey
                        }
                    }
                    1 -> {
                        setUpAnswerKey = setupRadioButtonOption(binding.option2, choice, question)
                        if (correctAns.not()) {
                            correctAns = setUpAnswerKey
                        }
                    }
                    2 -> {
                        setUpAnswerKey = setupRadioButtonOption(binding.option3, choice, question)
                        if (correctAns.not()) {
                            correctAns = setUpAnswerKey
                        }
                    }
                    3 -> {
                        setUpAnswerKey = setupRadioButtonOption(binding.option4, choice, question)
                        if (correctAns.not()) {
                            correctAns = setUpAnswerKey
                        }
                    }
                }
            }

            binding.submitAnswerBtn.isEnabled = false
            if (question.question.isAttempted) {
                if (correctAns) {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_check,
                        0,
                        0,
                        0
                    )
                } else {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_close_red_quiz,
                        0,
                        0,
                        0
                    )
                }
                binding.submitAnswerBtn.visibility = GONE
                question.reviseConcept?.let {
                    binding.showExplanationBtn.visibility = VISIBLE
                }
            } else {
                binding.showExplanationBtn.visibility = GONE
                binding.submitAnswerBtn.visibility = VISIBLE
                binding.continueBtn.visibility = GONE
                binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_check_grey,
                    0,
                    0,
                    0
                )
            }

        }

        private fun resetRadioBackground(radioButton: RadioButton) {
            radioButton.setBackgroundColor(
                ContextCompat.getColor(AppObjectController.joshApplication, R.color.white)
            )
            radioButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
            radioButton.elevation = 0F
        }

        private fun resetAllRadioButtonsBg() {
            binding.quizRadioGroup.children.iterator().forEach {
                if (it is RadioButton)
                    resetRadioBackground(it)
            }
        }

        //Sets up the view of radiobutton.
        private fun setupRadioButtonOption(
            radioButton: RadioButton,
            choice: Choice,
            question: AssessmentQuestionWithRelations
        ): Boolean {
            var isCorrectAns = false
            radioButton.text = choice.text
            if (question.question.isAttempted) {
                radioButton.isClickable = false

                if (choice.isCorrect) {
                    radioButton.setBackgroundResource(R.drawable.rb_correct_rect_bg)
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_green_tick,
                        0
                    )
                    radioButton.elevation = 4F
                    radioButton.alpha = 1f
                } else {
                    resetRadioBackground(radioButton)
                    radioButton.alpha = 0.5f
                }

                if (choice.userSelectedOrder == 1) {
                    if (choice.isCorrect)
                        isCorrectAns = true
                    binding.quizRadioGroup.setOnCheckedChangeListener(null)
                    radioButton.isChecked = true
                    binding.quizRadioGroup.setOnCheckedChangeListener(quizCheckedChangeListener)
                }
            } else {
                resetRadioBackground(radioButton)
                radioButton.isClickable = true
                radioButton.alpha = 1f
            }
            if (choice.isCorrect) {
                // Setting radiogroup tag = view id of correct radio button.
                // so that we can use it further to check if user has selected correct answer or not
                binding.quizRadioGroup.tag = radioButton.id
            }
            return isCorrectAns
        }
    }

    inner class VocabularyViewHolder(val binding: PracticeItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root),
        AudioPlayerEventListener,
        ExoAudioPlayer.ProgressUpdateListener {
        private var startTime: Long = 0L
        var filePath: String? = null
        private var mUserIsSeeking = false
        var lessonQuestion: LessonQuestion? = null
        var positionInList = -1

        init {
            initViewHolder()
        }

        private fun initViewHolder() {
            binding.handler = this
            binding.titleView.setOnClickListener {
                if (binding.practiceContentLl.visibility == GONE) {
                    expandCard()
                } else {
                    collapseCard()
                }
            }
            binding.btnPlayInfo.setOnClickListener {
                lessonQuestion?.let {
                    appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")
                        ?.addParam("lesson_id", it.lessonId)
                        ?.addParam("question_id", it.id)
                        ?.push()

                    playPracticeAudio(it, layoutPosition)
                }
            }

            binding.submitBtnPlayInfo.setOnClickListener {

                lessonQuestion?.let {
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_EXTRA.NAME,
                        "Already Submitted audio Played"
                    )
                        ?.addParam("lesson_id", it.lessonId)
                        ?.addParam("question_id", it.id)

                    playSubmitPracticeAudio(it, layoutPosition)
//                filePath = chatModel.downloadedLocalPath
                    val state =
                        if (it.isPlaying) {
                            currentQuestion?.isPlaying = true
                            MaterialPlayPauseDrawable.State.Pause
                        } else {
                            currentQuestion?.isPlaying = false
                            MaterialPlayPauseDrawable.State.Play
                        }

                    binding.submitBtnPlayInfo.state = state
                }
            }

            binding.submitPractiseSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        Log.d(
                            TAG,
                            "onStopTrackingTouch() called with: userSelectedPosition = $userSelectedPosition, userSelectedPosition.toLong() = ${userSelectedPosition.toLong()}, layoutPosition = $layoutPosition"
                        )
                        if (currentPlayingPosition == layoutPosition)
                            audioManager?.seekTo(userSelectedPosition.toLong())
                    }
                })

            binding.ivCancel.setOnClickListener {
                lessonQuestion?.let {
                    it.filePath = null
                    clickListener.cancelAudio()
                    removeAudioPractice(it)
                }
            }

            binding.submitAnswerBtn.setOnClickListener {
                Timber.d("Submit Button click started")
                lessonQuestion?.let {
                    expandCardPosition = positionInList + 1
                    if (it.filePath == null) {
                        showToast(AppObjectController.joshApplication.getString(R.string.submit_practise_msz))
                        return@setOnClickListener
                    }

                    val hasNextItem = positionInList < itemList.size - 1
                    if (clickListener.submitPractice(it, positionInList, hasNextItem)) {
                        if (isAudioPlaying()) {
                            binding.submitPractiseSeekbar.progress = 0
                            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                            it.isPlaying = false
                            audioManager?.resumeOrPause()
                        }
                        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                            ?.addParam("lesson_id", it.lessonId)
                            ?.addParam("question_id", it.id)
                            ?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Submitted")
                            ?.addParam(
                                AnalyticsEvent.PRACTICE_TYPE_SUBMITTED.NAME,
                                "$it Practice Submitted"
                            )
                            ?.addParam(
                                AnalyticsEvent.PRACTICE_SUBMITTED.NAME,
                                "Submit Practice $"
                            )
                            ?.push()
                    }
                }
                Timber.d("Submit Button click completed")
            }

            binding.videoPlayer.setPlayListener {
                lessonQuestion?.let {
                    val videoId = it.videoList?.getOrNull(0)?.id
                    val videoUrl = it.videoList?.getOrNull(0)?.video_url
                    val currentVideoProgressPosition = binding.videoPlayer.getProgress()
                    VideoPlayerActivity.startVideoActivity(
                        context,
                        "",
                        videoId,
                        videoUrl,
                        currentVideoProgressPosition,
                        conversationId = conversationId
                    )
                }
            }

            setAudioRecordTouchListener()

            binding.practiseSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        mUserIsSeeking = true
                    }

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        mUserIsSeeking = false
                        audioManager?.seekTo(userSelectedPosition.toLong())
                        //clickListener.onSeekChange(userSelectedPosition.toLong())
                    }
                })
        }

        fun bind(position: Int) {
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            if (expandCardPosition == positionInList && lessonQuestion?.status == QUESTION_STATUS.NA) {
                expandCardPosition = -1
                expandCard()
                if (position > 0)
                    clickListener.focusChild(position - 1)
            } else {
                collapseCard()
            }
            lessonQuestion?.let {
                setPracticeInfoView(it)
            }
        }

        //===============================
        override fun onPlayerPause() {
        }

        override fun onPlayerResume() {
        }

        override fun onCurrentTimeUpdated(lastPosition: Long) {
        }

        override fun onTrackChange(tag: String?) {
        }

        override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onPlayerReleased() {
        }

        override fun onPlayerEmptyTrack() {
        }

        override fun complete() {
            clickListener.playAudio(-1)
            pauseAudio()
            audioManager?.setProgressUpdateListener(null)
            audioManager?.seekTo(0)
            binding.progressBarImageView.progress = 0
            binding.practiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.progress = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            currentQuestion?.isPlaying = false
        }

        override fun onProgressUpdate(progress: Long) {
            lessonQuestion?.let {
                currentQuestion?.playProgress = progress.toInt()
                if (currentPlayingPosition != -1) {
                    binding.progressBarImageView.progress = progress.toInt()
                    if (it.materialType == LessonMaterialType.AU) {
                        binding.practiseSeekbar.progress = progress.toInt()
                    }
                    Timber.d("onProgressUpdate() called with: progress = $progress")
                    binding.submitPractiseSeekbar.progress = progress.toInt()

//                notifyItemChanged(layoutPosition)
                }
            }
        }

        override fun onDurationUpdate(duration: Long?) {
            if (duration != null && duration > 0) {
                duration.toInt().let { binding.submitPractiseSeekbar.max = it }
            }
        }

        private fun checkIsPlayer(): Boolean {
            return audioManager != null
        }

        private fun isAudioPlaying(): Boolean {
            return this.checkIsPlayer() && audioManager!!.isPlaying()
        }

        private fun onPlayAudio(
            lessonQuestion: LessonQuestion,
            audioObject: AudioType,
            position: Int
        ) {

            currentPlayingPosition = position

            currentQuestion = lessonQuestion
            val audioList = java.util.ArrayList<AudioType>()
            audioList.add(audioObject)
            audioManager?.playerListener = this
            audioManager?.play(audioObject.audio_url)
            audioManager?.setProgressUpdateListener(this)

            lessonQuestion.isPlaying = lessonQuestion.isPlaying.not()
            clickListener.playAudio(bindingAdapterPosition)
        }

        fun playPracticeAudio(lessonQuestion: LessonQuestion, position: Int) {
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                    .cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }

            if (currentQuestion == null) {
                lessonQuestion.audioList?.getOrNull(0)?.let {
                    onPlayAudio(lessonQuestion, it, position)
                }
            } else {
                if (currentQuestion == lessonQuestion) {
                    if (checkIsPlayer()) {
                        audioManager?.setProgressUpdateListener(this)
                        audioManager?.resumeOrPause()
                    } else {
                        onPlayAudio(
                            lessonQuestion,
                            lessonQuestion.audioList?.getOrNull(0)!!,
                            position
                        )
                    }
                } else {
                    onPlayAudio(
                        lessonQuestion,
                        lessonQuestion.audioList?.getOrNull(0)!!,
                        position
                    )
                }
            }
        }

        fun playSubmitPracticeAudio(lessonQuestion: LessonQuestion, position: Int) {
            try {
                val audioType = AudioType()
                if (filePath == null) {
                    if (lessonQuestion.practiceEngagement.isNullOrEmpty() && lessonQuestion.filePath != null) {
                        filePath = lessonQuestion.filePath
                    } else {
                        val practiseEngagement =
                            lessonQuestion.practiceEngagement?.getOrNull(0)
                        if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                                practiseEngagement?.localPath
                            )
                        ) {
                            filePath = practiseEngagement?.localPath
                        } else {
                            filePath = practiseEngagement?.answerUrl
                        }
                    }
                }
                audioType.audio_url = filePath!!
                audioType.downloadedLocalPath = filePath!!
                audioType.duration =
                    Utils.getDurationOfMedia(context, filePath!!)?.toInt() ?: 0
                audioType.id = nextInt().toString()

                binding.progressBarImageView.max = audioType.duration
                binding.practiseSeekbar.max = audioType.duration
                binding.submitPractiseSeekbar.max = audioType.duration
                if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                    StyleableToast.Builder(AppObjectController.joshApplication)
                        .gravity(Gravity.BOTTOM)
                        .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                        .cornerRadius(16)
                        .length(Toast.LENGTH_LONG)
                        .solidBackground().show()
                }

                if (currentQuestion == null) {
                    onPlayAudio(lessonQuestion, audioType, position)
                } else {
                    if (audioManager?.currentPlayingUrl?.isNotEmpty() == true && audioManager?.currentPlayingUrl == audioType.audio_url) {
                        if (checkIsPlayer()) {
                            currentPlayingPosition = position
                            audioManager?.setProgressUpdateListener(this)
                            lessonQuestion.isPlaying = lessonQuestion.isPlaying.not()
                            audioManager?.resumeOrPause()
//                            notifyItemChanged(layoutPosition)
                        } else {
                            onPlayAudio(lessonQuestion, audioType, position)
                        }
                    } else {
                        onPlayAudio(lessonQuestion, audioType, position)

                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

        fun pauseAudio() {
            audioManager?.onPause()
            lessonQuestion?.let {
                if (it.isPlaying) {
                    playSubmitPracticeAudio(it, layoutPosition)
                    currentQuestion?.isPlaying = false
                    binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                }
            }
        }

        //============================================================================
        private fun setPracticeInfoView(lessonQuestion: LessonQuestion) {
            val wordNumber = itemList.filter { it.assessmentId == null }.indexOf(lessonQuestion) + 1
            lessonQuestion.run {
                binding.practiceTitleTv.text =
                    AppObjectController.joshApplication.getString(
                        R.string.word_tag,
                        wordNumber,
                        wordsItemSize,
                        this.practiceWord
                    )
                if (this.status != QUESTION_STATUS.NA) {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_check,
                        0,
                        0,
                        0
                    )
                } else {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_check_grey,
                        0,
                        0,
                        0
                    )
                }
                when (this.materialType) {
                    LessonMaterialType.AU -> {
                        binding.audioViewContainer.visibility = VISIBLE
                        this.audioList?.getOrNull(0)?.audio_url?.let {
                            binding.btnPlayInfo.tag = it
                            binding.practiseSeekbar.max = this.audioList?.getOrNull(0)?.duration!!
                            if (binding.practiseSeekbar.max == 0) {
                                binding.practiseSeekbar.max = 2_00_000
                            }
                        }
                        initializePractiseSeekBar(lessonQuestion)

                    }
                    LessonMaterialType.IM -> {
                        binding.imageView.visibility = VISIBLE
                        this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                            binding.imageView.setImageAndFitCenter(path, context)
//                            binding.imageView.setOnClickListener {
//                                ImageShowFragment.newInstance(path, "", "")
//                                    .show(supportFragmentManager, "ImageShow")
//                            }
                        }
                    }
                    LessonMaterialType.VI -> {
                        binding.videoPlayer.visibility = VISIBLE
                        this.videoList?.getOrNull(0)?.video_url?.let {
                            binding.videoPlayer.setUrl(it)
                            binding.videoPlayer.fitToScreen()
//                            binding.videoPlayer.setPlayListener {
//                                val videoId = lessonQuestion.videoList?.getOrNull(0)?.id
//                                val videoUrl = lessonQuestion.videoList?.getOrNull(0)?.video_url
//                            val currentVideoProgressPosition = binding.videoPlayer.getProgress()
//                                VideoPlayerActivity.startVideoActivity(
//                                    context,
//                                    "",
//                                    videoId,
//                                    videoUrl,
//                                    currentVideoProgressPosition
//                                )
//                            }
                            binding.videoPlayer.downloadStreamButNotPlay()
                        }
                    }
                    LessonMaterialType.TX -> {
                        this.qText?.let {
                            binding.infoTv.visibility = VISIBLE
                            binding.infoTv.text =
                                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        }
                    }
                    else -> {

                    }
                }

                if (this.materialType != LessonMaterialType.TX &&
                    this.qText.isNullOrEmpty().not()
                ) {
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.infoTv2.visibility = VISIBLE
                }

                if (this.status == QUESTION_STATUS.NA) {
                    binding.submitAnswerBtn.visibility = VISIBLE
                    setViewAccordingExpectedAnswer(lessonQuestion)
                } else {
                    hidePracticeInputLayout()
                    binding.submitAnswerBtn.visibility = GONE
                    setViewUserSubmitAnswer(lessonQuestion)
                }

            }
        }

        private fun setViewAccordingExpectedAnswer(lessonQuestion: LessonQuestion) {
            lessonQuestion.run {
                showPracticeInputLayout()
                this.expectedEngageType?.let {
                    binding.uploadPractiseView.visibility = VISIBLE
                    binding.practiseInputHeader.text = AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.READING_PRACTICE_TITLE)
                    binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                    binding.audioPractiseHint.visibility = VISIBLE
                    binding.submitAudioViewContainer.visibility = GONE
                    binding.yourSubAnswerTv.visibility = GONE
                }
            }
        }

        private fun setViewUserSubmitAnswer(lessonQuestion: LessonQuestion) {
            lessonQuestion.expectedEngageType?.let {
                hidePracticeInputLayout()
                showPracticeSubmitLayout()
                binding.yourSubAnswerTv.visibility = VISIBLE
                val params: ViewGroup.MarginLayoutParams =
                    binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = Utils.dpToPx(20)
                binding.subPractiseSubmitLayout.layoutParams = params
                binding.yourSubAnswerTv.text =
                    AppObjectController.joshApplication.getString(R.string.your_submitted_answer)
                if (lessonQuestion.practiceEngagement.isNullOrEmpty() && lessonQuestion.status == QUESTION_STATUS.IP) {
                    filePath = lessonQuestion.filePath
                    binding.submitPractiseSeekbar.max =
                        Utils.getDurationOfMedia(context, filePath!!)
                            ?.toInt() ?: 1_00_000
                } else {
                    val practiseEngagement = lessonQuestion.practiceEngagement?.getOrNull(0)
                    if (EXPECTED_ENGAGE_TYPE.AU == it) {
                        binding.submitAudioViewContainer.visibility = VISIBLE
                    }
                    if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                            practiseEngagement?.localPath
                        )
                    ) {
                        filePath = practiseEngagement?.localPath
                        binding.submitPractiseSeekbar.max =
                            Utils.getDurationOfMedia(context, filePath!!)
                                ?.toInt() ?: 0
                    } else {
                        filePath = practiseEngagement?.answerUrl
                        if (practiseEngagement?.duration != null || practiseEngagement?.duration == 0) {
                            binding.submitPractiseSeekbar.max = practiseEngagement.duration!!
                        } else {
                            binding.submitPractiseSeekbar.max = 1_00_000
                        }
                    }
                }

                initializePractiseSeekBar(lessonQuestion)
                binding.ivCancel.visibility = GONE
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setAudioRecordTouchListener() {
            binding.uploadPractiseView.setOnTouchListener { _, event ->
                if (PermissionUtils.isAudioAndStoragePermissionEnable(context).not()) {
                    clickListener.askRecordPermission()
                    return@setOnTouchListener true
                }
                lessonQuestion?.let {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            binding.videoPlayer.onPause()
                            binding.practiseInfoLayout.requestDisallowInterceptTouchEvent(true)
                            binding.counterContainer.visibility = VISIBLE
                            val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale)
                            binding.uploadPractiseView.startAnimation(scaleAnimation)
                            binding.counterTv.base = SystemClock.elapsedRealtime()

                            binding.counterTv.start()
                            this.startTime = System.currentTimeMillis()
                            clickListener.startRecording(it, layoutPosition, startTime)
                            binding.audioPractiseHint.visibility = GONE

                            appAnalytics?.addParam(
                                AnalyticsEvent.AUDIO_RECORD.NAME,
                                "Audio Recording"
                            )
                                ?.addParam("lesson_id", it.lessonId)
                                ?.addParam("question_id", it.id)
                                ?.push()
                        }
                        MotionEvent.ACTION_MOVE -> {
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            binding.practiseInfoLayout.requestDisallowInterceptTouchEvent(false)
                            binding.counterTv.stop()
                            val stopTime = System.currentTimeMillis()
//                        stopRecording(chatModel, stopTime)
                            clickListener.stopRecording(it, layoutPosition, stopTime)
                            binding.uploadPractiseView.clearAnimation()
                            binding.counterContainer.visibility = GONE
                            binding.audioPractiseHint.visibility = VISIBLE
                            binding.ivCancel.visibility = VISIBLE
//                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            val timeDifference =
                                TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                                    startTime
                                )
                            if (timeDifference > 1) {
                                audioAttachmentInit(it)
                            } else {

                            }
                        }
                        else -> {

                        }
                    }
                }
                true
            }
        }

        private fun audioAttachmentInit(lessonQuestion: LessonQuestion) {
            CoroutineScope(Dispatchers.Main).launch {
                showPracticeSubmitLayout()
                binding.submitAudioViewContainer.visibility = VISIBLE
                delay(300)
                initializePractiseSeekBar(lessonQuestion)
                if (filePath == null) {
                    if (lessonQuestion.practiceEngagement.isNullOrEmpty() && lessonQuestion.filePath != null) {
                        filePath = lessonQuestion.filePath
                    } else {
                        val practiseEngagement =
                            lessonQuestion.practiceEngagement?.getOrNull(0)
                        if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                                practiseEngagement?.localPath
                            )
                        ) {
                            filePath = practiseEngagement?.localPath
                        } else {
                            filePath = practiseEngagement?.answerUrl
                        }
                    }
                } else {
                    filePath = lessonQuestion.filePath
                }
                binding.submitPractiseSeekbar.max =
                    Utils.getDurationOfMedia(context, filePath)?.toInt() ?: 0
                enableSubmitButton()
            }
        }

        private fun initializePractiseSeekBar(lessonQuestion: LessonQuestion) {
            binding.practiseSeekbar.progress = lessonQuestion.playProgress
        }

        private fun removeAudioPractice(lessonQuestion: LessonQuestion) {
            if (isAudioPlaying()) {
                audioManager?.resumeOrPause()
            }
            hidePracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = GONE
            binding.submitPractiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.max = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            disableSubmitButton()
            appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")
                ?.addParam("lesson_id", lessonQuestion.lessonId)
                ?.addParam("question_id", lessonQuestion.id)
                ?.push()
        }

        private fun disableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = false
                isClickable = false
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.seek_bar_background
                    )
                )
            }
        }

        private fun enableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = true
                isClickable = true
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.button_color
                    )
                )
            }
            requestFocus(
                binding.submitAnswerBtn
            )
        }

        private fun requestFocus(view: View) {
            Handler().postDelayed({
                view.parent.requestChildFocus(
                    view,
                    view
                )
            }, 200)
        }

        private fun hidePracticeInputLayout() {
            binding.practiseInputHeader.visibility = GONE
            binding.practiceInputLl.visibility = GONE
        }

        private fun showPracticeInputLayout() {
            binding.practiseInputHeader.visibility = VISIBLE
            binding.practiceInputLl.visibility = VISIBLE
        }

        private fun showPracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = VISIBLE
            binding.subPractiseSubmitLayout.visibility = VISIBLE
        }

        private fun hidePracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = GONE
            binding.subPractiseSubmitLayout.visibility = GONE
        }

        private fun expandCard() {
            binding.practiceContentLl.visibility = VISIBLE
            binding.expandIv.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_remove
                )
            )
        }

        private fun collapseCard() {
            binding.practiceContentLl.visibility = GONE
            binding.expandIv.setImageDrawable(
                ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.ic_add
                )
            )
        }
    }

    interface PracticeClickListeners {
        fun submitPractice(
            lessonQuestion: LessonQuestion,
            positionInList: Int,
            hasNextItem: Boolean
        ): Boolean

        fun startRecording(lessonQuestion: LessonQuestion, position: Int, startTimeUnit: Long)
        fun stopRecording(lessonQuestion: LessonQuestion, position: Int, stopTime: Long)
        fun askRecordPermission()
        fun focusChild(position: Int)
        fun submitQuiz(
            lessonQuestion: LessonQuestion,
            isCorrect: Boolean,
            questionId: Int,
            positionInList: Int,
            hasNextItem: Boolean,
            canShowSectionCompletedCard: Boolean = true
        )

        fun openNextScreen(canShowSectionCompletedCard: Boolean = true)
        fun quizOptionSelected(
            lessonQuestion: LessonQuestion,
            assessmentQuestion: AssessmentQuestionWithRelations,
            positionInList: Int,
            hasNextItem: Boolean
        )

        fun playAudio(position: Int)
        fun cancelAudio()
    }
}
