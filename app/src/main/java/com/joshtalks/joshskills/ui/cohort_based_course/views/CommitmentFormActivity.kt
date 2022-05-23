package com.joshtalks.joshskills.ui.cohort_based_course.views

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.databinding.ActivityCommitmentFormBinding
import com.joshtalks.joshskills.ui.cohort_based_course.viewmodels.CommitmentFormViewModel


class CommitmentFormActivity : BaseActivity() {

    private val binding by lazy<ActivityCommitmentFormBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_commitment_form)
    }

    val vm by lazy {
        ViewModelProvider(this)[CommitmentFormViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun onCreated() {
        addCommitmentFormLaunchFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                OPEN_PROMISE_FRAGMENT -> replaceWithPromiseFragment()
                OPEN_SCHEDULE_FRAGMENT -> replaceWithScheduleFragment()
            }
        }
    }

    private fun replaceWithScheduleFragment() {
        supportFragmentManager.commit {
            replace(R.id.commitment_form_container, ScheduleFragment(), "ScheduleFragment")
        }
    }

    private fun replaceWithPromiseFragment() {
        supportFragmentManager.commit {
            replace(R.id.commitment_form_container, PromiseFragment(), "PromiseFragment")
        }
    }

    private fun addCommitmentFormLaunchFragment() {
        supportFragmentManager.commit {
            add(R.id.commitment_form_container, CommitmentFormLaunchFragment(), "CommitmentFormLaunchFragment")
        }
    }
}