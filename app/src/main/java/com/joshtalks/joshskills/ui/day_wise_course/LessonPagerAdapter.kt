package com.joshtalks.joshskills.ui.day_wise_course

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.day_wise_course.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.day_wise_course.practice.NewPracticeFragment
import com.joshtalks.joshskills.ui.day_wise_course.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.day_wise_course.spaking.SpeakingPractiseFragment

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    val lifecycle: Lifecycle,
    val chatList: ArrayList<ArrayList<ChatModel>>,
    val courseId: String,
    val lessonId: Int

) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {

        return when (position) {
            0 -> GrammarFragment.instance(chatList[0])

            1 -> NewPracticeFragment.instance(chatList[1])

            2 -> ReadingFragmentWithoutFeedback.instance(chatList[2])

            else -> SpeakingPractiseFragment.newInstance(
                courseId = courseId,
                lessonId = lessonId,
                topicId = getTopicId(),
                questionId = getQuestionId()
            )
        }
    }

    private fun getTopicId(): String? {
        return chatList.getOrNull(3)
            ?.find { obj -> obj.question?.topicId != null }?.question?.topicId
    }

    private fun getQuestionId(): String? {
        return chatList.getOrNull(3)
            ?.find { obj -> obj.question?.topicId != null }?.question?.questionId
    }
}