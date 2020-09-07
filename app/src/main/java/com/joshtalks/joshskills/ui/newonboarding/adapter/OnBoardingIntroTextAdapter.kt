package com.joshtalks.joshskills.ui.newonboarding.adapter


import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.repository.server.onboarding.Content
import com.joshtalks.joshskills.ui.newonboarding.fragment.OnBoardIntroTextFragment

class OnBoardingIntroTextAdapter(
    fm: FragmentManager, private val contentList: List<Content>,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : FragmentStatePagerAdapter(fm, behavior) {
    override fun getCount(): Int = contentList.size

    override fun getItem(position: Int): OnBoardIntroTextFragment =
        OnBoardIntroTextFragment.newInstance(contentList[position].text,contentList[position].description)

}
