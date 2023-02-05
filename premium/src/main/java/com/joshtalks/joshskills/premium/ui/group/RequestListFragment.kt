package com.joshtalks.joshskills.premium.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseFragment
import com.joshtalks.joshskills.premium.databinding.RequestListFragmentBinding
import com.joshtalks.joshskills.premium.track.CONVERSATION_ID
import com.joshtalks.joshskills.premium.ui.group.constants.GROUPS_ID
import com.joshtalks.joshskills.premium.ui.group.viewmodels.GroupRequestViewModel

private const val TAG = "RequestListFragment"

class RequestListFragment : BaseFragment() {
    lateinit var binding: RequestListFragmentBinding

    val vm by lazy {
        ViewModelProvider(this)[GroupRequestViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.request_list_fragment, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {}

    override fun setArguments() {
        arguments.let { args ->
            vm.groupId = args?.getString(GROUPS_ID, "") ?: ""
            vm.conversationId = args?.getString(CONVERSATION_ID, "") ?: ""
        }
        vm.getRequestList()
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }
}