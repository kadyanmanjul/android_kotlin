package com.joshtalks.joshskills.ui.day_wise_course.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.day_wise_course.fragments.NewPracticeFragment
import com.joshtalks.joshskills.ui.day_wise_course.fragments.ReadingFragment
import com.joshtalks.joshskills.ui.day_wise_course.grammar.GrammarFragment

class LessonPagerAdapter(
    val chatModel: ChatModel,
    fragmentManager: FragmentManager,
    val lifecycle: Lifecycle
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GrammarFragment()
            1 -> {
                val chatList = ArrayList<ChatModel>()
                chatList.add(chatModel)
                chatList.add(chatModel)
                chatList.add(chatModel)
                chatList.add(chatModel)
                NewPracticeFragment.instance(chatList)
            }
            else -> ReadingFragment()
        }
    }
}