package com.joshtalks.joshskills.ui.group.views

import android.os.Bundle
import android.util.Log

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider

import com.afollestad.materialdialogs.MaterialDialog

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.constants.SEARCH_GROUP
import com.joshtalks.joshskills.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.constants.DISMISS_PROGRESS_BAR
import com.joshtalks.joshskills.constants.OPEN_GROUP_REQUEST
import com.joshtalks.joshskills.constants.REFRESH_GRP_LIST_HIDE_INFO
import com.joshtalks.joshskills.core.MOENGAGE_USER_CREATED
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivityJoshVoipGroupctivityBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.*
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity

import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.moengage.core.analytics.MoEAnalyticsHelper

private const val TAG = "JoshVoipGroupActivity"

class JoshVoipGroupActivity : BaseGroupActivity() {

    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    val binding by lazy<ActivityJoshVoipGroupctivityBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_josh_voip_groupctivity)
    }

    override fun setIntentExtras() {}

    override fun initViewBinding() {
        binding.vm = vm
        vm.isFromVoip.set(true)
        vm.newGroupVisible.set(true)
        binding.executePendingBindings()
    }

    override fun onCreated() {
        openGroupListFragment()
        initMoEnagageForGroups()
    }

    private fun initMoEnagageForGroups() {
        if (!PrefManager.getBoolValue(MOENGAGE_USER_CREATED)) {
            vm.initializeMoEngageUser()
            PrefManager.put(MOENGAGE_USER_CREATED, true)
            MoEAnalyticsHelper.setUniqueId(this, Mentor.getInstance().getId())
        }
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                ON_BACK_PRESSED -> onBackPressed()
                OPEN_GROUP -> {
                    if (supportFragmentManager.backStackEntryCount == 0)
                        startGroupCall(it.obj as? GroupItemData)
                    else
                        openGroupChat(it.obj as? GroupItemData)
                }
                SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
                SEARCH_GROUP -> openGroupSearchFragment()
                SHOW_PROGRESS_BAR -> showProgressDialog(it.obj as String)
                DISMISS_PROGRESS_BAR -> dismissProgressDialog()
                OPEN_GROUP_REQUEST -> openGroupRequestFragment()
                REFRESH_GRP_LIST_HIDE_INFO -> {
                    vm.hasGroupData.set(it.data.getBoolean(SHOW_NEW_INFO))
                    vm.hasGroupData.notifyChange()
                    vm.setGroupsCount()
                }
            }
        }
    }

    private fun openGroupListFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, GroupListFragment(), LIST_FRAGMENT)
        }
    }

    private fun startGroupCall(groupItemData: GroupItemData?) {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            openCallingActivity(groupItemData)
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                this@JoshVoipGroupActivity,
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            openCallingActivity(groupItemData)
                            return
                        } else {
                            MaterialDialog(this@JoshVoipGroupActivity).show {
                                message(R.string.call_start_permission_message)
                                positiveButton(R.string.ok)
                            }
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    override fun getConversationId(): String? {
        vm.conversationId = intent.getStringExtra(CONVERSATION_ID) ?: ""
        return vm.conversationId
    }

    fun openCallingActivity(groupItemData: GroupItemData?) {
        // TODO: Might need to refactor
        val memberText = groupItemData?.getSubTitle() ?: "0"
        val memberCount = getMemberCount(memberText)
        if (memberCount == 0) {
            com.joshtalks.joshskills.core.showToast("Unknown Error Occurred")
            return
        } else if (memberCount == 1) {
            com.joshtalks.joshskills.core.showToast("You are the only member, Can't Initiate a Call")
            return
        }
        GroupAnalytics.push(GroupAnalytics.Event.CALL_PRACTICE_PARTNER, groupItemData?.getUniqueId() ?: "")
        val intent = SearchingUserActivity.startUserForPractiseOnPhoneActivity(
            this,
            courseId = "151",
            topicId = 5,
            groupId = groupItemData?.getUniqueId(),
            isGroupCallCall = true,
            topicName = "Group Call",
            favoriteUserCall = false,
            groupName = groupItemData?.getTitle()
        )
        startActivity(intent)
        finish()
    }

    private fun openGroupChat(data: GroupItemData?) {
        if (data?.getJoinedStatus() == NOT_JOINED_GROUP && data.getGroupCategory() == CLOSED_GROUP) {
            openGroupRequestFragment(data)
            return
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putString(GROUPS_CREATOR, data?.getCreator())
                putString(GROUPS_TITLE, data?.getTitle())
                putString(GROUPS_CHAT_SUB_TITLE, data?.getSubTitle())
                putString(GROUPS_IMAGE, data?.getImageUrl())
                putString(GROUPS_ID, data?.getUniqueId())
                putString(CONVERSATION_ID, vm.conversationId)
                putString(ADMIN_ID, data?.getCreatorId())
                putString(GROUP_TYPE, data?.getGroupCategory())
                putString(GROUP_STATUS, data?.getJoinedStatus())
                putString(CLOSED_GROUP_TEXT, data?.getGroupText())
                data?.hasJoined()?.let { putBoolean(HAS_JOINED_GROUP, it) }
            }

            val fragment = GroupChatFragment()
            fragment.arguments = bundle
            replace(R.id.group_fragment_container, fragment, CHAT_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openGroupRequestFragment(data: GroupItemData? = null) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)

            val bundle = Bundle().apply {
                putString(GROUPS_TITLE, data?.getTitle())
                putString(GROUPS_IMAGE, data?.getImageUrl())
                putString(GROUPS_ID, data?.getUniqueId())
                putString(CLOSED_GROUP_TEXT, data?.getGroupText())
            }

            val fragment = GroupRequestFragment()
            fragment.arguments = bundle
            add(R.id.group_fragment_container, fragment, GROUP_REQUEST_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openGroupSearchFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putBoolean(IS_FROM_VOIP, true)
                putString(CONVERSATION_ID, vm.conversationId)
            }
            val fragment = GroupSearchFragment().apply {
                arguments = bundle
            }
            replace(R.id.group_fragment_container, fragment, SEARCH_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }
}