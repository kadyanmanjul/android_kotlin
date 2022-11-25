package com.joshtalks.joshskills.common.ui.cohort_based_course.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.BaseFragment
import com.joshtalks.joshskills.common.databinding.FragmentScheduleBinding
import com.joshtalks.joshskills.common.ui.cohort_based_course.viewmodels.CommitmentFormViewModel

class ScheduleFragment: com.joshtalks.joshskills.common.base.BaseFragment() {

    lateinit var binding: FragmentScheduleBinding

    val vm by lazy {
        ViewModelProvider(requireActivity()).get(CommitmentFormViewModel::class.java)
    }

    override fun initViewBinding() {
        binding.vm=vm
        binding.executePendingBindings()
    }

    override fun initViewState() {}

    override fun setArguments() {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_schedule, container, false)
        return binding.root
    }
}