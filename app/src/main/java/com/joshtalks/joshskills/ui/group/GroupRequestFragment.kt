package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.REQUEST_GROUP_VALIDATION
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.databinding.GroupRequestFragmentBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.constants.CLOSED_GROUP_TEXT
import com.joshtalks.joshskills.ui.group.constants.GROUPS_ID
import com.joshtalks.joshskills.ui.group.constants.GROUPS_IMAGE
import com.joshtalks.joshskills.ui.group.constants.GROUPS_TITLE
import com.joshtalks.joshskills.base.model.groups.GroupJoinRequest
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
        vm.requestQuestion.set(AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REQUEST_TO_JOIN_QUESTION))
        binding.executePendingBindings()

        binding.answerText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                REQUEST_GROUP_VALIDATION -> {
                    if (binding.rulesCheck.isChecked) {
                        val answer = binding.answerText.text.toString().trim()
                        if (answer.isNotBlank()) {
                            val request = GroupJoinRequest(
                                mentorId = Mentor.getInstance().getId(),
                                groupId = vm.groupId,
                                answer = answer
                            )
                            vm.joinPrivateGroup(request)
                        } else
                            showToast("Please answer the question")
                    } else {
                        showToast("Please accept the group rules")
                    }
                }
            }
        }
    }

    override fun setArguments() {
        arguments?.let { args ->
            vm.conversationId = args.getString(CONVERSATION_ID, "") ?: ""

            vm.imageUrl.set(args.getString(GROUPS_IMAGE, ""))
            vm.groupHeader.set(args.getString(GROUPS_TITLE, ""))
            vm.groupId = args.getString(GROUPS_ID, "")
            vm.groupText = args.getString(CLOSED_GROUP_TEXT, "")
        }
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }
}