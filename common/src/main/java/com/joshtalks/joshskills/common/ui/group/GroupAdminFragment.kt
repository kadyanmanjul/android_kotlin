package com.joshtalks.joshskills.common.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.BaseFragment
import com.joshtalks.joshskills.common.constants.ADD_GROUP_TO_SERVER
import com.joshtalks.joshskills.common.databinding.FragmentGroupAdminBinding
import com.joshtalks.joshskills.common.ui.group.constants.ADD_GROUP_REQUEST
import com.joshtalks.joshskills.common.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.common.ui.group.viewmodels.JoshGroupViewModel

private const val TAG = "GroupAdminFragment"

class GroupAdminFragment : com.joshtalks.joshskills.common.base.BaseFragment() {
    lateinit var binding: FragmentGroupAdminBinding
    private lateinit var addGroupRequest: AddGroupRequest

    val vm by lazy {
        ViewModelProvider(requireActivity())[JoshGroupViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_admin, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                com.joshtalks.joshskills.common.constants.ADD_GROUP_TO_SERVER -> {
                    if (binding.adminCheck.isChecked) {
                        vm.addGroup(addGroupRequest)
                    } else
                        showToast("Please accept the admin responsibilities")
                }
            }
        }
    }

    override fun setArguments() {
        arguments?.let {
            addGroupRequest = it.getParcelable(ADD_GROUP_REQUEST)!!
        }
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }
}