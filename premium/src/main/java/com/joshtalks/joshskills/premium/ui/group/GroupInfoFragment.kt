package com.joshtalks.joshskills.premium.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseFragment
import com.joshtalks.joshskills.premium.databinding.GroupInfoFragmentBinding
import com.joshtalks.joshskills.premium.ui.group.viewmodels.GroupChatViewModel

private const val TAG = "GroupInfoFragment"

class GroupInfoFragment : BaseFragment() {
    lateinit var binding: GroupInfoFragmentBinding

    val vm by lazy {
        ViewModelProvider(requireActivity())[GroupChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.group_info_fragment, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
        vm.memberAdapter.shouldShowAll(false)
    }

    override fun initViewState() {}

    override fun setArguments() {}

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }
}