package com.joshtalks.joshskills.ui.lesson

import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS

interface LessonActivityListener {

    fun onNextTabCall(currentTabNumber: Int)

    fun onQuestionStatusUpdate(
        status: QUESTION_STATUS,
        questionId: String?,
        isVideoPercentComplete: Boolean = false,
        quizCorrectQuestionIds: ArrayList<Int> = ArrayList()
    )

    fun onSectionStatusUpdate(tabPosition: Int, isSectionCompleted: Boolean)

    fun onLessonUpdate()
    fun showVideoToolTip(
        shouldShow: Boolean,
        wrongAnswerHeading: String? = null,
        wrongAnswerHeading2: String? = null,
        wrongAnswerText1: String? = null,
        wrongAnswerText2: String? = null,
        videoClickListener: (() -> Unit)? = null
    )
}
