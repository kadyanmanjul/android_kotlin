package com.joshtalks.joshskills.lesson.online_test.util

import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentQuestionWithRelations

interface GrammarSubmitButtonListener {
    fun toggleSubmitButton(isEnabled: Boolean)
    fun toggleLoading(isLoading: Boolean)
    fun playAudio(audioUrl: String?, localAudioPath: String?)
}

interface AssessmentQuestionViewCallback {
    var assessmentQuestion: AssessmentQuestionWithRelations?
    fun setup(assessmentQuestion: AssessmentQuestionWithRelations)
    fun lockViews()
    fun unlockViews()
    fun getAnswerText(): String
    fun isAnyAnswerSelected(): Boolean
    private fun checkAnswerInList(answer: String, listOfAnswers: List<String>?): Boolean {
        listOfAnswers?.let {
            for (i in it)
                if (answer.trim().equals(i.trim(), ignoreCase = true))
                    return true
        }
        return false
    }

    fun isAnswerCorrect(): Boolean {
        lockViews()
        return if (isAnyAnswerSelected().not()) {
            unlockViews()
            false
        } else {
            checkAnswerInList(
                answer = getAnswerText(),
                listOfAnswers = assessmentQuestion?.question?.listOfAnswers
            ).also {
                assessmentQuestion?.question?.isCorrect = it
            }
        }
    }

    fun addGrammarTestCallback(callback: GrammarSubmitButtonListener)
}

interface GrammarButtonViewCallback {
    fun onGrammarButtonClick()
    fun showTooltip(
        wrongAnswerTitle: String?,
        wrongAnswerDescription: String?,
        explanationTitle: String?,
        explanationText: String?,
    )

    fun onVideoButtonClicked()
}
