package com.joshtalks.joshskills.ui.day_wise_course

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.ui.day_wise_course.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.day_wise_course.practice.NewPracticeFragment
import com.joshtalks.joshskills.ui.day_wise_course.reading.ReadingFragment

class LessonPagerAdapter(
    val questionList: ArrayList<Question>,
    fragmentManager: FragmentManager,
    val lifecycle: Lifecycle
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    var grammarQuestions: ArrayList<ChatModel> = ArrayList()
    var vocabularyQuestions: ArrayList<ChatModel> = ArrayList()
    var readingQuestions: ArrayList<ChatModel> = ArrayList()

    init {
        questionList.forEach {
            val chatModel = ChatModel()
            chatModel.question = it
            when (it.chatType) {
                CHAT_TYPE.GR -> {
                    grammarQuestions.add(chatModel)
                }
                CHAT_TYPE.RP -> {
                    readingQuestions.add(chatModel)
                }
                CHAT_TYPE.VP -> {
                    vocabularyQuestions.add(chatModel)
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

            2 -> ReadingFragment.instance(readingQuestions)

            else -> ReadingFragment.instance(readingQuestions)
        }
    }
}