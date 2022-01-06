package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup

import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.paging.map

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.INIT_LIST_TOOLTIP
import com.joshtalks.joshskills.constants.OPEN_POPUP_MENU
import com.joshtalks.joshskills.core.HAS_SEEN_GROUP_TOOLTIP
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.FragmentGroupListBinding
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics.Event.*
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

private const val TAG = "GroupListFragment"

class GroupListFragment : BaseFragment() {
    lateinit var binding: FragmentGroupListBinding
    val vm by lazy {
        ViewModelProvider(requireActivity())[JoshGroupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            vm.setGroupsCount()
            if (vm.isFromVoip.get()) {
                withContext(Dispatchers.IO) {
                    vm.getGroupOnlineCount()
                    vm.getGroupLocalData().let {
                        val groupList = it.map { data ->
                            val countDetails = vm.groupMemberCounts[data.groupId]
                            data.lastMessage = "${countDetails?.memberCount} members, ${countDetails?.onlineCount} online"
                            data.unreadCount = "0"
                            data as GroupItemData
                        }
                        withContext(Dispatchers.Main) { vm.adapter.submitData(PagingData.from(groupList)) }
                    }
                }
            } else {
                vm.getGroupData().distinctUntilChanged().collectLatest {
                    Log.d(TAG, "onCreate: $it")
                    withContext(Dispatchers.IO) {
                        val groupList = it.map { data -> data as GroupItemData }
                        Log.d(TAG, "onCreate: $groupList")
                        withContext(Dispatchers.Main) {
                            vm.adapter.submitData(groupList)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_list, container, false)
        return binding.root
    }

    private fun initTooltip() {
        if (!PrefManager.getBoolValue(HAS_SEEN_GROUP_TOOLTIP)) {
            binding.animLayout.visibility = VISIBLE
            binding.overlayGroupTooltip.visibility = VISIBLE
            binding.overlayLayout.visibility = VISIBLE

            PrefManager.put(HAS_SEEN_GROUP_TOOLTIP, true)

            binding.overlayLayout.setOnClickListener {
                binding.animLayout.visibility = GONE
                binding.overlayGroupTooltip.visibility = GONE
                binding.overlayLayout.visibility = GONE
                binding.overlayLayout.setOnClickListener(null)
            }
        }
    }

    override fun initViewBinding() {
        binding.let {
            binding.vm = vm
            binding.executePendingBindings()
        }
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                OPEN_POPUP_MENU -> openPopupMenu()
                INIT_LIST_TOOLTIP -> initTooltip()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (vm.shouldRefreshGroupList) {
            vm.shouldRefreshGroupList = false
            vm.adapter.refresh()
        }
    }

    override fun setArguments() {}

    private fun openPopupMenu() {
        val popupMenu = PopupMenu(
            requireContext(),
            binding.groupAppBar.secondIconImageView,
            R.style.setting_menu_style
        )
        popupMenu.inflate(R.menu.groups_menu)
        popupMenu.setOnMenuItemClickListener {
            popupMenu.dismiss()
            when (it.itemId) {
                R.id.new_group -> {
                    vm.openNewGroup()
                }
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }
}