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
}
