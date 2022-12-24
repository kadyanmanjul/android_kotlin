package com.joshtalks.joshskills.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.lesson.vocabulary.VocabularyFragment

class LessonPagerTestAdapter(fm: FragmentManager, behavior: Int) :
    FragmentStatePagerAdapter(fm, behavior) {

    override fun getCount() = 4

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> GrammarFragment.getInstance()

        1 -> VocabularyFragment.getInstance()

        2 -> ReadingFragmentWithoutFeedback.getInstance()

        else -> SpeakingPractiseFragment.newInstance()
    }

    /*override fun getPageTitle(position: Int): CharSequence? = when (position) {
        0 -> "Grammar"

        1 -> "Vocabulary"

        2 -> "Reading"

        else -> "Speaking"
    }*/
}