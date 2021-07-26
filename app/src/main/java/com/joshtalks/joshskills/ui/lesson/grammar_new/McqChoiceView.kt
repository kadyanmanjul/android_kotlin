package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import com.muddzdev.styleabletoast.StyleableToast
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class McqChoiceView : RadioGroup, AudioPlayerEventListener {

    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    lateinit var mcqOptionsRadioGroup: RadioGroup
    private var callback: EnableDisableGrammarButtonCallback? = null
    var audioManager: ExoAudioPlayer2? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.mcq_option_group, this)
        mcqOptionsRadioGroup = findViewById(R.id.mcq_options_radio_group)
        audioManager = ExoAudioPlayer2.getInstance()
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        mcqOptionsRadioGroup.removeAllViews()
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                val optionView = getOptionView(it)
                mcqOptionsRadioGroup.addView(optionView)
            }
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
            delay(500)
            if (assessmentQuestion.question.isAttempted) {
                callback?.alreadyAttempted(isCorrectAnswer())
            }
        }
    }

    private fun getOptionView(choice: Choice): McqOptionView {
        val optionView = McqOptionView(context, choice)
        optionView.setOnClickListener(ClickListener())
        return optionView
    }

    private fun lockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).setEnabled(false)
        }
    }

    private fun unlockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).setEnabled(true)
        }
    }

    fun isAnyAnswerSelected(): Boolean {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            if ((mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).choice.isSelectedByUser) {
                return true
            }
        }
        return false
    }

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            assessmentQuestion?.question?.isAttempted = true
            assessmentQuestion?.choiceList?.forEach {
                if (it.isSelectedByUser != it.isCorrect) {
                    return false
                }
            }
        }
        return true
    }

    fun playAudio(audioUrl: String?) {

        audioUrl?.let { url ->
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) ?: 0 <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                    .cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }
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
        audioManager?.play(
            audioObject.audio_url,
            playbackSpeed = AppObjectController.getFirebaseRemoteConfig()
                .getDouble(FirebaseRemoteConfigKey.GRAMMAR_CHOICE_PLAYBACK_SPEED).toFloat()
        )
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

    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

    private inner class ClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val clickedOptionView = view as McqOptionView?
            for (i in 0 until mcqOptionsRadioGroup.childCount) {
                val optionView = (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView)
                if (optionView == clickedOptionView) {
                    optionView.changeState()
                    playAudio(optionView.choice.audioUrl)
                } else {
                    optionView.setState(McqOptionState.UNSELECTED)
                }
                if (isAnyAnswerSelected()) {
                    callback?.enableGrammarButton()
                } else {
                    callback?.disableGrammarButton()
                }
            }
        }
    }

}
