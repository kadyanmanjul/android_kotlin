package com.joshtalks.joshskills.ui.newonboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.ui.newonboarding.fragment.OnBoardIntroFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectCourseFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectInterestFragment
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel

class OnBoardingActivityNew : CoreJoshActivity() {

    lateinit var viewModel: OnBoardViewModel

    companion object {
        const val FLOW_FROM_INBOX = "FLOW_FROM_INBOX"

        fun startOnBoardingActivity(
            context: Activity, requestCode: Int, flowFromInbox: Boolean = false
        ) {
            val intent = Intent(context, OnBoardingActivityNew::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(FLOW_FROM_INBOX, flowFromInbox)
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_new)
        if (intent.hasExtra(FLOW_FROM_INBOX)) {
            if (intent.getBooleanExtra(FLOW_FROM_INBOX, false)) {
                openCoursesFragment()
            } else {
                openOnBoardingIntroFragment()
            }
        } else openOnBoardingIntroFragment()

    }

    private fun openCoursesFragment() {
        when (getVersionData()?.version!!.name) {
            ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                this.finish()
            }
            ONBOARD_VERSIONS.ONBOARDING_V2 -> {
                replaceFragment(
                    R.id.onboarding_container,
                    SelectCourseFragment.newInstance(true),
                    SelectCourseFragment.TAG
                )
            }
            ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V3 -> {
                replaceFragment(
                    R.id.onboarding_container,
                    SelectInterestFragment.newInstance(),
                    SelectInterestFragment.TAG
                )
            }
        }
    }

    protected fun openOnBoardingIntroFragment() {
        replaceFragment(
            R.id.onboarding_container,
            OnBoardIntroFragment.newInstance(),
            OnBoardIntroFragment.TAG
        )
        viewModel = ViewModelProvider(this).get(OnBoardViewModel::class.java)
    }
}
