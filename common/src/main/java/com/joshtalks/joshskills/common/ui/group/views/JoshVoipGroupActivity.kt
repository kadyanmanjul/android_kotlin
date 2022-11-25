package com.joshtalks.joshskills.common.ui.group.views

import android.content.Intent
import android.os.Bundle

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider

import com.afollestad.materialdialogs.MaterialDialog

import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.constants.*
import com.joshtalks.joshskills.common.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.common.constants.OPEN_GROUP
import com.joshtalks.joshskills.common.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.common.constants.SEARCH_GROUP
import com.joshtalks.joshskills.common.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.common.constants.DISMISS_PROGRESS_BAR
import com.joshtalks.joshskills.common.constants.OPEN_GROUP_REQUEST
import com.joshtalks.joshskills.common.constants.REFRESH_GRP_LIST_HIDE_INFO
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.databinding.ActivityJoshVoipGroupctivityBinding
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.common.ui.group.*
import com.joshtalks.joshskills.common.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.common.ui.group.constants.*
import com.joshtalks.joshskills.common.ui.group.model.GroupItemData
import com.joshtalks.joshskills.common.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.common.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.common.voip.constant.Category

import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.lang.Exception

private const val TAG = "JoshVoipGroupActivity"

class JoshVoipGroupActivity : com.joshtalks.joshskills.common.ui.group.BaseGroupActivity() {

    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    val binding by lazy<ActivityJoshVoipGroupctivityBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_josh_voip_groupctivity)
    }

    override fun setIntentExtras() {
        AppObjectController.initGroups()
    }

    override fun initViewBinding() {
        binding.vm = vm
        vm.isFromVoip.set(true)
        vm.newGroupVisible.set(true)
        binding.executePendingBindings()
    }

    override fun onCreated() {
        openGroupListFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                com.joshtalks.joshskills.common.constants.ON_BACK_PRESSED -> popBackStack()
                com.joshtalks.joshskills.common.constants.OPEN_GROUP -> {
                    if (supportFragmentManager.backStackEntryCount == 0)
                        startGroupCall(it.obj as? GroupItemData)
                    else
                        openGroupChat(it.obj as? GroupItemData)
                }
                com.joshtalks.joshskills.common.constants.SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
                com.joshtalks.joshskills.common.constants.SEARCH_GROUP -> openGroupSearchFragment()
                com.joshtalks.joshskills.common.constants.SHOW_PROGRESS_BAR -> showProgressDialog(it.obj as String)
                com.joshtalks.joshskills.common.constants.DISMISS_PROGRESS_BAR -> dismissProgressDialog()
                com.joshtalks.joshskills.common.constants.OPEN_GROUP_REQUEST -> openGroupRequestFragment()
                com.joshtalks.joshskills.common.constants.REFRESH_GRP_LIST_HIDE_INFO -> {
                    vm.hasGroupData.set(it.data.getBoolean(SHOW_NEW_INFO))
                    vm.hasGroupData.notifyChange()
                    vm.setGroupsCount()
                }
            }
        }
    }

    private fun popBackStack() {
        try {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressed()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        vm.conversationId = intent.getStringExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID) ?: ""
        return vm.conversationId
    }

    fun openCallingActivity(groupItemData: GroupItemData?) {
        // TODO: Might need to refactor
        val memberText = groupItemData?.getSubTitle() ?: "0"
        val memberCount = getMemberCount(memberText)
        if (memberCount == 0) {
            showToast("Unknown Error Occurred")
            return
        } else if (memberCount == 1) {
            showToast("You are the only member, Can't Initiate a Call")
            return
        }
        GroupAnalytics.push(GroupAnalytics.Event.CALL_PRACTICE_PARTNER, groupItemData?.getUniqueId() ?: "")
        val callIntent = Intent(applicationContext, VoiceCallActivity::class.java)
        callIntent.apply {
            putExtra(
                STARTING_POINT,
                FROM_ACTIVITY
            )
            putExtra(INTENT_DATA_CALL_CATEGORY, Category.GROUP.ordinal)
            putExtra(INTENT_DATA_GROUP_ID, groupItemData?.getUniqueId())
            putExtra(INTENT_DATA_TOPIC_ID, "5")
            putExtra(INTENT_DATA_GROUP_NAME, groupItemData?.getTitle())
        }
        startActivity(callIntent)
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
                putString(com.joshtalks.joshskills.common.track.CONVERSATION_ID, vm.conversationId)
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
                putString(com.joshtalks.joshskills.common.track.CONVERSATION_ID, vm.conversationId)
            }
            val fragment = GroupSearchFragment().apply {
                arguments = bundle
            }
            replace(R.id.group_fragment_container, fragment, SEARCH_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }
}