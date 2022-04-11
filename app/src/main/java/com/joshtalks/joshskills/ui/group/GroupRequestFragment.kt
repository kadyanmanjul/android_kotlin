package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.REQUEST_GROUP_VALIDATION
import com.joshtalks.joshskills.databinding.GroupRequestFragmentBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.model.GroupJoinRequest
import com.joshtalks.joshskills.ui.group.viewmodels.GroupChatViewModel

private const val TAG = "RequestListFragment"

class GroupRequestFragment : BaseFragment() {
    lateinit var binding: GroupRequestFragmentBinding

    val vm by lazy {
        ViewModelProvider(requireActivity())[GroupChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.group_request_fragment, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                REQUEST_GROUP_VALIDATION -> {
                    if (binding.rulesCheck.isChecked) {
                        val answer = binding.answerText.text.toString()
                        if (answer.isNotBlank()) {
                            val request = GroupJoinRequest(
                                mentorId = Mentor.getInstance().getId(),
                                groupId = vm.groupId,
                                answer = answer
                            )
                            vm.joinPrivateGroup(request)
                        }
                    } else {
                        showToast("Please accept the group rules")
                    }
                }
            }
        }
    }

    override fun setArguments() {
        arguments.let { args ->
            vm.conversationId = args?.getString(CONVERSATION_ID, "") ?: ""
        }
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }
}