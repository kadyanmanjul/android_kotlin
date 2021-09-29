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

    fun setOverlayVisibility(
        shouldShow: Boolean,
        wrongAnswerHeading: String?,
        wrongAnswerText: String?,
        videoTitle: String?,
        videoId: String?,
        videoUrl: String?
    )
}
