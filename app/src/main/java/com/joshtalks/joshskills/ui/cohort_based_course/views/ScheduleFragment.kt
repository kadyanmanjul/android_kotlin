package com.joshtalks.joshskills.ui.cohort_based_course.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentScheduleBinding
import com.joshtalks.joshskills.ui.cohort_based_course.adapters.ScheduleAdapter
import com.joshtalks.joshskills.ui.cohort_based_course.viewmodels.CommitmentFormViewModel

class ScheduleFragment: BaseFragment() {

    lateinit var binding: FragmentScheduleBinding

    val vm by lazy {
        ViewModelProvider(this)[CommitmentFormViewModel::class.java]
    }

    override fun initViewBinding() {
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

        val timeList = listOf("10:00 PM - 11:00 PM","12:00 PM - 01:00 PM", // demo time slots
            "02:00 PM - 03:00 PM","04:00 PM - 05:00 PM","06:00 PM - 07:00 PM","08:00 PM - 09:00 PM")
        val adapter = ScheduleAdapter(timeList)
        binding.recyclerView2.adapter = adapter
        binding.recyclerView2.layoutManager = GridLayoutManager(context,2)

        return binding.root
    }

}