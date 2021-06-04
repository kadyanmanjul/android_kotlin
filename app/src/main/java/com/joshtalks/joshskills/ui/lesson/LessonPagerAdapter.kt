package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val fragmentList:List<Fragment>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList.get(position)
        /*return when (position) {
            //0 -> GrammarFragment.getInstance()
            0 -> {
                if (isNewGrammar) {
                    GrammarOnlineTestFragment.getInstance(lessonNumber)
                } else {
                    GrammarFragment.getInstance()
                }
            }

            1 -> VocabularyFragment.getInstance()

            2 -> ReadingFragmentWithoutFeedback.getInstance()

            else -> SpeakingPractiseFragment.newInstance()
        }*/
    }
}
