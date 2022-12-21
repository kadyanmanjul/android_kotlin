package com.joshtalks.joshskills.userprofile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.userprofile.R
import com.joshtalks.joshskills.userprofile.databinding.FragmentEnrolledCoursesBinding

class EnrolledCoursesFragment : com.joshtalks.joshskills.common.base.BaseFragment() {
    lateinit var binding: FragmentEnrolledCoursesBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(com.joshtalks.joshskills.userprofile.viewmodel.UserProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_enrolled_courses, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.let {
            binding.vm = viewModel
            binding.executePendingBindings()
        }
    }

    override fun initViewState() {}

    override fun setArguments() {}

    override fun onPause() {
        viewModel.saveImpression()
        super.onPause()
    }
}
