package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.lesson.vocabulary.VocabularyFragment

class LessonPagerAdapter(fm: FragmentManager, behavior: Int) :
    FragmentStatePagerAdapter(fm, behavior) {

    override fun getCount() = 4

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> GrammarFragment.getInstance()

        1 -> VocabularyFragment.getInstance()

        2 -> ReadingFragmentWithoutFeedback.getInstance()

        else -> SpeakingPractiseFragment.newInstance()
    }
}
