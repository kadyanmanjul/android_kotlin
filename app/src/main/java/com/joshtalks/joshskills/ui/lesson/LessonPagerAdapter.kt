package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.lesson.vocabulary.VocabularyFragment
import com.joshtalks.joshskills.ui.online_test.GrammarOnlineTestFragment

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val isNewGrammar: Boolean = false,
    val lessonNumber: Int
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
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
        }
    }
}
