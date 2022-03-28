package com.joshtalks.joshskills.ui.userprofile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentMyGroupsBinding
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel

class MyGroupsFragment : BaseFragment() {
    lateinit var binding: FragmentMyGroupsBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(UserProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_my_groups,
            container,
            false
        )
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
