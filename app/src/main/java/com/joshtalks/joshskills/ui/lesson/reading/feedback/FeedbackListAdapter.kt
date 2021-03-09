package com.joshtalks.joshskills.ui.lesson.reading.feedback


import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2

const val MAX_ATTEMPT = 4

class FeedbackListAdapter(
    fm: Fragment,
    private val practiceEngagementV2: List<PracticeEngagementV2>
) : FragmentStateAdapter(fm) {
    override fun getItemCount(): Int {
        return practiceEngagementV2.size
    }

    override fun createFragment(position: Int): Fragment {
        return RecordAndFeedbackFragment.newInstance(practiceEngagementV2[position], isImprove())
    }

    private fun isImprove(): Boolean {
        if (practiceEngagementV2.size == MAX_ATTEMPT) {
            return false
        }
        return true
    }

    fun isAnyPractiseUploading(): Boolean {
        if (DOWNLOAD_STATUS.UPLOADING == practiceEngagementV2.last().uploadStatus) {
            return true
        }
        return false
    }
}