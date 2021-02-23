package com.joshtalks.joshskills.ui.day_wise_course

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.ui.day_wise_course.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.day_wise_course.spaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.day_wise_course.vocabulary.VocabularyFragment
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GrammarFragment.getInstance()

            1 -> VocabularyFragment.getInstance()

            2 -> ReadingFragmentWithoutFeedback.getInstance()

            else -> SpeakingPractiseFragment.newInstance()
        }
    }
}
