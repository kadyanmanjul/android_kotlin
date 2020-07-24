package com.joshtalks.joshskills.ui.conversation_practice.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.fragment.ListenPractiseFragment
import com.joshtalks.joshskills.ui.conversation_practice.fragment.QuizPractiseFragment
import com.joshtalks.joshskills.ui.conversation_practice.fragment.RecordPractiseFragment
import com.joshtalks.joshskills.ui.conversation_practice.fragment.SelfPractiseFragment


class PractiseViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val conversationPractiseModel: ConversationPractiseModel
) :
    FragmentStateAdapter(fragmentActivity) {
    private val mFragmentNames = arrayOf("First Tab", "SecondTab", "third", "fourth")


    override fun getItemCount(): Int {
        return mFragmentNames.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> {
                QuizPractiseFragment.newInstance(conversationPractiseModel.quizModel)
            }
            2 -> {
                SelfPractiseFragment.newInstance(conversationPractiseModel)
            }
            3 -> {
                RecordPractiseFragment.newInstance(conversationPractiseModel)
            }
            else -> {
                ListenPractiseFragment.newInstance(conversationPractiseModel.listen)
            }
        }
    }

}
