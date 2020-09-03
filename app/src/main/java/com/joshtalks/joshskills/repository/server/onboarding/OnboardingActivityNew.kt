package com.joshtalks.joshskills.repository.server.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.ui.new_onboarding.onboarding2.SelectInterestFragment

class OnboardingActivityNew : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, OnboardingActivityNew::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_new)

        replaceFragment(
            R.id.onboarding_container,
            SelectInterestFragment.newInstance(5, 3),
            SelectInterestFragment.TAG
        )

    }

}