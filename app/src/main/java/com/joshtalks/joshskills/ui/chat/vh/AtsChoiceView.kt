package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomLayout
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import com.muddzdev.styleabletoast.StyleableToast
import com.nex3z.flowlayout.FlowLayout
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AtsChoiceView : RelativeLayout, AudioPlayerEventListener {

    private lateinit var rootView: RelativeLayout
    private lateinit var answerContainer: FrameLayout
    private lateinit var answerFlowLayout: FlowLayout
    private lateinit var dummyAnswerFlowLayout: FlowLayout
    private lateinit var optionsContainer: RelativeLayout
    private lateinit var customLayout: CustomLayout
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var callback: EnableDisableGrammarButtonCallback? = null
    var audioManager: ExoAudioPlayer2? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.grammar_choice_view, this)
        rootView = findViewById(R.id.choice_ats_root_view)
        answerContainer = findViewById(R.id.ats_answer_container)
        answerFlowLayout = findViewById(R.id.ats_answer_flow_layout)
        dummyAnswerFlowLayout = findViewById(R.id.dummy_answer_flow_layout)
        optionsContainer = findViewById(R.id.ats_options_container)
        initOptionsFlowLayout()
        audioManager = ExoAudioPlayer2.getInstance()
    }

    private fun initOptionsFlowLayout() {
        customLayout = CustomLayout(context)
        customLayout.setGravity(Gravity.CENTER)
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        optionsContainer.addView(customLayout, params)
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
        customLayout.removeAllViews()
        answerFlowLayout.removeAllViews()
        val selectedWords = ArrayList<CustomWord>()
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                val wordView = getWordView(it)
                addChoiceToOptionsLayout(wordView)
                if (it.userSelectedOrder != 0 && it.userSelectedOrder != 100) {
                    selectedWords.add(wordView)
                }
            }

        selectedWords.sortedBy { it.choice.userSelectedOrder }.forEach {
            it.changeViewGroup(customLayout, answerFlowLayout)
        }

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
            delay(500)
            if (assessmentQuestion.question.isAttempted) {
                callback?.alreadyAttempted(isCorrectAnswer())
            }
        }

        if (customLayout.childCount > 9) {
            addDummyLineView(3)
        } else {
            addDummyLineView(2)
        }

    }

    private fun addChoiceToOptionsLayout(word: CustomWord) {
        customLayout.push(word)
    }

    private fun addChoiceToAnswerLayout(word: CustomWord) {
        answerFlowLayout.addView(word)
    }

    private fun getWordView(choice: Choice): CustomWord {
        val customWord = CustomWord(context, choice)
        customWord.setOnClickListener(ClickListener())
        return customWord
    }

    private fun lockViews() {
        for (i in 0 until answerFlowLayout.childCount) {
            (answerFlowLayout.getChildAt(i) as CustomWord).setEnabled(false)
        }
        for (i in 0 until customLayout.childCount) {
            (customLayout.getChildAt(i) as CustomWord).setEnabled(false)
        }
    }

    private fun unlockViews() {
        for (i in 0 until answerFlowLayout.childCount) {
            (answerFlowLayout.getChildAt(i) as CustomWord).setEnabled(true)
        }
        for (i in 0 until customLayout.childCount) {
            (customLayout.getChildAt(i) as CustomWord).setEnabled(true)
        }
    }

    fun isAnyAnswerSelected() = answerFlowLayout.childCount > 0

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            assessmentQuestion?.question?.isAttempted = true
            assessmentQuestion?.choiceList?.forEach {
                if ((it.correctAnswerOrder == 0 || it.correctAnswerOrder == 100) &&
                    (it.userSelectedOrder != 0 || it.userSelectedOrder != 100)
                ) {
                    return false
                }
                if (it.correctAnswerOrder != 0 &&
                    it.correctAnswerOrder != 100 &&
                    it.userSelectedOrder != it.correctAnswerOrder
                ) {
                    return false
                }
            }
        }
        return true
    }

    fun playAudio(audioUrl: String?) {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) ?: 0 <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                .cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }

        val audioUrl2=audioUrl?.replace(" ".toRegex(), "%20")

        audioUrl2?.let { url ->
            if (isAudioPlaying()) {
                audioManager?.onPause()
            }
            val audioType = AudioType()
            audioType.audio_url = url
            audioType.downloadedLocalPath = url
            audioType.duration = 1_00
            audioType.id = Random.nextInt().toString()
            onPlayAudio(audioType)
        }
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(
        audioObject: AudioType
    ) {
        audioManager?.playerListener = this
        audioManager?.play(audioObject.audio_url)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
    }

    private inner class ClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            if (!customLayout.isEmpty()) {
                val customWord = view as CustomWord
                if (customWord.parent is CustomLayout) {
                    playAudio(customWord.choice.audioUrl)
                }
                customWord.changeViewGroup(customLayout, answerFlowLayout)
                if (isAnyAnswerSelected()) {
                    callback?.enableGrammarButton()
                } else {
                    callback?.disableGrammarButton()
                }
            }
        }
    }

    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

    fun addDummyLineView(numberOfLines: Int) {
        dummyAnswerFlowLayout.removeAllViews()
        for (i in 1..numberOfLines) {
            val dummyWordView = CustomWord(context)
            val wordLayoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            wordLayoutParams.gravity = Gravity.CENTER
            wordLayoutParams.setMargins(
                Utils.dpToPx(5),
                Utils.dpToPx(10),
                Utils.dpToPx(5),
                Utils.dpToPx(10)
            )
            dummyWordView.setLayoutParams(wordLayoutParams)
            dummyWordView.visibility = INVISIBLE
            dummyAnswerFlowLayout.addView(dummyWordView)
            val line = View(context)
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.dpToPx(1)
            )
            layoutParams.setMargins(
                0,
                Utils.dpToPx(14),
                0,
                Utils.dpToPx(0)
            )
            line.setLayoutParams(layoutParams)
            line.background = ContextCompat.getDrawable(context, R.color.light_shade_of_gray)
            dummyAnswerFlowLayout.addView(line)

        }
    }

}

interface EnableDisableGrammarButtonCallback {
    fun disableGrammarButton()
    fun enableGrammarButton()
    fun alreadyAttempted(isCorrectAnswer: Boolean)
}
