package com.joshtalks.joshskills.ui.group

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentGroupListBinding
import com.joshtalks.joshskills.ui.group.constants.DM_CHAT
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GROUP_LIST_CBC_TOOLTIP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
            if (vm.isFromVoip.get()) {
                vm.setGroupsCount()
                loadListWhenFromVoip()
            } else {
                vm.getGroupData().distinctUntilChanged().collectLatest {
                    Log.d(TAG, "onCreate: $it")
                    vm.setGroupsCount()
                    withContext(Dispatchers.IO) {
                        val groupList = it.map { data -> data as GroupItemData }
                        withContext(Dispatchers.Main) {
                            vm.adapter.submitData(groupList)
                        }
                    }
                }
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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenStarted {
            if (vm.isFromVoip.get())
                loadListWhenFromVoip()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_list, container, false)
        return binding.root
    }

    private fun initTooltip() {
        if (!PrefManager.getBoolValue(HAS_SEEN_GROUP_TOOLTIP) && !vm.isFromVoip.get()
            && PrefManager.getBoolValue(HAS_SEEN_GROUP_LIST_CBC_TOOLTIP)
        ) {
            if (AppObjectController.getFirebaseRemoteConfig()
                    .getBoolean(FirebaseRemoteConfigKey.SHOW_NEW_GROUP_BTN)
            ) {
                binding.overlayGroupTooltip.setTooltipText("You can search groups and create new group from here")
                binding.overlayNewGroup.visibility = VISIBLE
            }

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

        lifecycleScope.launch(Dispatchers.Main) {
            if (vm.getClosedGroupCount() == 0 &&
                !PrefManager.getBoolValue(ONE_GROUP_REQUEST_SENT) &&
                (vm.groupListCount.get() ?: 0) < 3)
                binding.bellsGroup.visibility = VISIBLE
        }
    }

    override fun initViewBinding() {
        binding.let {
            binding.vm = vm
            if (!AppObjectController.getFirebaseRemoteConfig().getBoolean(FirebaseRemoteConfigKey.SHOW_NEW_GROUP_BTN))
                vm.newGroupVisible.set(true)
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

    private suspend fun loadListWhenFromVoip() {
        withContext(Dispatchers.IO) {
            vm.getGroupOnlineCount()
            vm.getGroupLocalData().let {
                val groupList = it.map { data ->
                    val countDetails = vm.groupMemberCounts[data.groupId]
                    data.lastMessage = "${countDetails?.memberCount} members, ${countDetails?.onlineCount} online"
                    data.unreadCount = "0"
                    data as GroupItemData
                }
                groupList.filter { group ->
                    group.getGroupCategory() != DM_CHAT
                }
                withContext(Dispatchers.Main) { vm.adapter.submitData(PagingData.from(groupList)) }
            }
        }
    }

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        requireActivity().window.decorView.getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = requireActivity().window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
        val titleBarHeight = contentViewTop - statusBarHeight
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }
}