package com.joshtalks.joshskills.ui.day_wise_course

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.day_wise_course.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.day_wise_course.practice.NewPracticeFragment
import com.joshtalks.joshskills.ui.day_wise_course.reading.ReadingFragment
import com.joshtalks.joshskills.ui.day_wise_course.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.day_wise_course.spaking.SpeakingPractiseFragment

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    val lifecycle: Lifecycle,
    val chatList: List<ChatModel>,
    val courseId: String,
    val lessonId: Int

) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    var grammarQuestions: ArrayList<ChatModel> = ArrayList()
    var vocabularyQuestions: ArrayList<ChatModel> = ArrayList()
    var readingQuestions: ArrayList<ChatModel> = ArrayList()

    init {
        chatList.forEach {
            when (it.question?.chatType) {
                CHAT_TYPE.GR -> {
                    grammarQuestions.add(it)
                }
                CHAT_TYPE.RP -> {
                    readingQuestions.add(it)
                }
                CHAT_TYPE.VP -> {
                    vocabularyQuestions.add(it)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {

        return when (position) {
            0 -> GrammarFragment.instance(grammarQuestions)

            1 -> NewPracticeFragment.instance(vocabularyQuestions)

            2 -> ReadingFragmentWithoutFeedback.instance(readingQuestions)

            else -> SpeakingPractiseFragment.newInstance(
                courseId = courseId,
                lessonId = lessonId,
                topicId = getTopicId(),
                questionId = getQuestionId()
            )
        }
    }

    private fun getTopicId(): String? {
        return chatList.find { obj -> obj.question?.topicId != null }?.question?.topicId
    }

    private fun getQuestionId(): String? {
        return chatList.find { obj -> obj.question?.topicId != null }?.question?.questionId
    }
}