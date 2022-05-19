package com.joshtalks.joshskills.ui.cohort_based_course.views

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityCommitmentFormBinding
import com.joshtalks.joshskills.ui.cohort_based_course.viewmodels.CommitmentFormViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.CallFragment

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
        TODO("Not yet implemented")
    }

    override fun initViewState() {
        TODO("Not yet implemented")
    }

    private fun addCallUserFragment() {
        supportFragmentManager.commit {
            add(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }
}