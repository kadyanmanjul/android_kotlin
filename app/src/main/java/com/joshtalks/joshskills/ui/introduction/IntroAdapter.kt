package com.joshtalks.joshskills.ui.introduction

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.introduction.DemoOnboardingData

class IntroAdapter(
    val data: DemoOnboardingData,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return data.screenList?.get(position)?.let { PageFragment.newInstance(it) }!!
    }
}


