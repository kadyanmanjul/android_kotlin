package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLEAR_CHAT_TEXT
import com.joshtalks.joshskills.constants.OPEN_EMOJI_KEYBOARD
import com.joshtalks.joshskills.constants.OPEN_GROUP_INFO
import com.joshtalks.joshskills.constants.NEW_CHAT_ADDED
import com.joshtalks.joshskills.constants.SEND_MSG
import com.joshtalks.joshskills.core.HAS_SEEN_GROUP_CALL_TOOLTIP
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.hideKeyboard
import com.joshtalks.joshskills.databinding.GroupChatFragmentBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.viewmodels.GroupChatViewModel

import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GroupChatFragment"

class GroupChatFragment : BaseFragment() {
    lateinit var binding: GroupChatFragmentBinding
    lateinit var emojiPopup: EmojiPopup

    val vm by lazy {
        ViewModelProvider(requireActivity())[GroupChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.group_chat_fragment, container, false)
        return binding.root
    }

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()
        lifecycleScope.launchWhenStarted {
            vm.getChatData().distinctUntilChanged().collectLatest {
                withContext(Dispatchers.Main) { vm.chatAdapter.submitData(it) }
            }
        }
    }

    private fun initTooltip() {
        if (!PrefManager.getBoolValue(HAS_SEEN_GROUP_CALL_TOOLTIP)) {
            binding.animLayout.visibility = VISIBLE
            binding.overlayGroupTooltip.visibility = VISIBLE
            binding.overlayLayout.visibility = VISIBLE

            PrefManager.put(HAS_SEEN_GROUP_CALL_TOOLTIP, true)

            binding.overlayLayout.setOnClickListener {
                binding.animLayout.visibility = GONE
                binding.overlayGroupTooltip.visibility = GONE
                binding.overlayLayout.visibility = GONE
                binding.overlayLayout.setOnClickListener(null)
            }
        }
    }

    override fun initViewBinding() {
        init()
        binding.vm = vm
        binding.executePendingBindings()
        vm.memberCount.set(0)
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                OPEN_EMOJI_KEYBOARD -> openEmojiKeyboard(it.data)
                CLEAR_CHAT_TEXT -> binding.groupChatSendMsg.setText("")
                SEND_MSG -> vm.pushMessage(binding.groupChatSendMsg.text.toString().trim())
                OPEN_GROUP_INFO -> hideKeyboard(requireContext())
                NEW_CHAT_ADDED -> {
                    if (it.data.getInt(GROUP_CHAT_UNREAD) != 0) {
                        binding.groupChatRv.layoutManager?.scrollToPosition(
                            it.data.getInt(GROUP_CHAT_UNREAD)
                        )
                    } else {
                        val lastItemPosition = (binding.groupChatRv.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                        if (vm.scrollToEnd || lastItemPosition == 0) {
                            binding.groupChatRv.layoutManager?.scrollToPosition(0)
                            binding.scrollUnread.visibility = INVISIBLE
                        } else binding.scrollUnread.visibility = VISIBLE
                        vm.scrollToEnd = false
                    }
                }
            }
        }
    }

    override fun setArguments() {
        arguments?.let { args ->
            vm.groupHeader.set(args.getString(GROUPS_TITLE, ""))
            vm.hasJoinedGroup.set(args.getBoolean(HAS_JOINED_GROUP, false))
            vm.groupSubHeader.set(args.getString(GROUPS_CHAT_SUB_TITLE, ""))
            vm.groupId = args.getString(GROUPS_ID, "")
            vm.imageUrl.set(args.getString(GROUPS_IMAGE, ""))
            vm.groupCreator.set(args.getString(GROUPS_CREATOR, ""))
            vm.conversationId = args.getString(CONVERSATION_ID, "") ?: ""
            vm.adminId = args.getString(ADMIN_ID, "")
            vm.groupType.set(args.getString(GROUP_TYPE, OPENED_GROUP))
            vm.groupJoinStatus.set(vm.getGroupJoinText(args.getString(GROUP_STATUS, JOINED_GROUP)))
            args.getInt(GROUP_CHAT_UNREAD, 0).let {
                vm.unreadCount = it
                if (it != 0) {
                    vm.setUnreadLabel(it)
                }
            }
            if (vm.hasJoinedGroup.get()) {
                vm.getGroupInfo()
                initTooltip()
            }
        }
    }

    private fun init() {
        val rootView = binding.rootContainer
        val emojiIconEditText = binding.groupChatSendMsg
        emojiPopup = EmojiPopup.Builder.fromRootView(rootView).build(emojiIconEditText)

        binding.scrollToEndButton.setOnClickListener {
            scrollToEnd()
        }
    }

    private fun scrollToEnd() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.groupChatRv.layoutManager?.scrollToPosition(
                (binding.groupChatRv.adapter?.itemCount!!) - 1
            )
            binding.scrollToEndButton.visibility = GONE
        }
    }

    private fun openEmojiKeyboard(data: Bundle) {
        val emojiButton = binding.chatEmojiBtn
        if (emojiPopup.isShowing || data.getBoolean(IS_FROM_KEYBOARD)) {
            emojiPopup.dismiss()
            emojiButton.setImageResource(R.drawable.ic_chat_emoji)
        } else {
            emojiPopup.toggle()
            emojiButton.setImageResource(R.drawable.ic_keyboard)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshGroupInfo()
    }

    override fun onPause() {
        super.onPause()
        vm.resetUnreadAndTimeToken()
        vm.resetUnreadLabel()
    }

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            vm.chatAdapter.submitData(PagingData.empty())
        }
        super.onDestroy()
    }
}