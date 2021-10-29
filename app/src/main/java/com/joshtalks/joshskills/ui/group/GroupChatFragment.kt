package com.joshtalks.joshskills.ui.group

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.GroupChatFragmentBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.viewmodels.GroupChatViewModel
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity

private const val TAG = "GroupChatFragment"
class GroupChatFragment : BaseFragment() {
    lateinit var binding : GroupChatFragmentBinding

    val vm by lazy {
        ViewModelProvider(this)[GroupChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.group_chat_fragment, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun getConversationId(): String? {
        return if(vm.conversationId.isBlank()) null else vm.conversationId
    }

    override fun initViewState() {}

    override fun setArguments() {
        arguments?.let {
            vm.groupHeader.set(it.getString(GROUPS_TITLE,""))
            vm.hasJoinedGroup.set(it.getBoolean(HAS_JOINED_GROUP, false))
            vm.groupSubHeader.set(it.getString(GROUPS_CHAT_SUB_TITLE, ""))
            vm.groupId = it.getString(GROUPS_ID, "")
            vm.imageUrl.set(it.getString(GROUPS_IMAGE, ""))
            vm.groupCreatedAt.set(it.getString(GROUPS_CREATED_TIME, ""))
            vm.groupCreator.set(it.getString(GROUPS_CREATOR, ""))
            vm.conversationId = it.getString(CONVERSATION_ID, "") ?: ""
        }
    }

    override fun onResume() {
        super.onResume()
        vm.getOnlineUserCount()
    }
}