package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLEAR_CHAT_TEXT
import com.joshtalks.joshskills.constants.OPEN_EMOJI_KEYBOARD
import com.joshtalks.joshskills.constants.SEND_MSG
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.HAS_SEEN_GROUP_CALL_TOOLTIP
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.GroupChatFragmentBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.viewmodels.GroupChatViewModel

import com.vanniktech.emoji.EmojiPopup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GroupChatFragment"

class GroupChatFragment : BaseFragment() {
    lateinit var binding: GroupChatFragmentBinding
    lateinit var emojiPopup: EmojiPopup
    private val database = AppObjectController.appDatabase

    val vm by lazy {
        ViewModelProvider(requireActivity())[GroupChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.group_chat_fragment, container, false)
        init()
        initTooltip()
        return binding.root
    }

    private fun initTooltip() {
        if (!PrefManager.getBoolValue(HAS_SEEN_GROUP_CALL_TOOLTIP)) {
            binding.animLayout.visibility = View.VISIBLE
            binding.overlayGroupTooltip.visibility = View.VISIBLE
            binding.overlayLayout.visibility = View.VISIBLE

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
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                OPEN_EMOJI_KEYBOARD -> {
                    openEmojiKeyboard(it.data)
                }
                CLEAR_CHAT_TEXT -> binding.groupChatSendMsg.setText("")
                SEND_MSG -> vm.pushMessage(binding.groupChatSendMsg.text.toString())
            }
        }
    }

    override fun setArguments() {
        arguments?.let {
            vm.groupHeader.set(it.getString(GROUPS_TITLE, ""))
            vm.hasJoinedGroup.set(it.getBoolean(HAS_JOINED_GROUP, false))
            vm.groupSubHeader.set(it.getString(GROUPS_CHAT_SUB_TITLE, ""))
            vm.groupId = it.getString(GROUPS_ID, "")
            vm.imageUrl.set(it.getString(GROUPS_IMAGE, ""))
            vm.groupCreatedAt.set(it.getString(GROUPS_CREATED_TIME, ""))
            vm.groupCreator.set(it.getString(GROUPS_CREATOR, ""))
            vm.conversationId = it.getString(CONVERSATION_ID, "") ?: ""
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
        vm.getOnlineUserCount()
        lifecycleScope.launch(Dispatchers.Main) {
            val item = database.groupListDao().getGroupItem(vm.groupId)
            if (item.name != null) {
                vm.groupHeader.set(item.name)
                vm.imageUrl.set(item.groupIcon)
            }
        }
    }
}