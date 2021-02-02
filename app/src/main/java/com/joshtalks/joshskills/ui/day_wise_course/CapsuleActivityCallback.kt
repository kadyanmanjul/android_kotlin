package com.joshtalks.joshskills.ui.day_wise_course

import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS

interface CapsuleActivityCallback {
    fun onNextTabCall(currentTabNumber: Int)
    fun onQuestionStatusUpdate(
        status: QUESTION_STATUS,
        questionId: Int,
        isVideoPercentComplete: Boolean = false,
        quizCorrectQuestionIds: ArrayList<Int> = ArrayList()
    )

    fun onSectionStatusUpdate(tabPosition: Int, status: Boolean)
}