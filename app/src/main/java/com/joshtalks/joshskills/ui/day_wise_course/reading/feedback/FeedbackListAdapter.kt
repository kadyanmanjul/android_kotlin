package com.joshtalks.joshskills.ui.day_wise_course.reading.feedback


import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2

class FeedbackListAdapter(
    fm: Fragment,
    private val practiceEngagementV2: List<PracticeEngagementV2>
) : FragmentStateAdapter(fm) {
    override fun getItemCount(): Int {
        return practiceEngagementV2.size
    }

    override fun createFragment(position: Int): Fragment {
        return RecordAndFeedbackFragment.newInstance((practiceEngagementV2[position]))
    }
}