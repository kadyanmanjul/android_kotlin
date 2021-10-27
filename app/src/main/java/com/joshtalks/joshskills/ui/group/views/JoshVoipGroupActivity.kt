package com.joshtalks.joshskills.ui.group.views

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.SEARCH_GROUP
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.databinding.ActivityJoshVoipGroupctivityBinding
import com.joshtalks.joshskills.ui.group.CHAT_FRAGMENT
import com.joshtalks.joshskills.ui.group.GROUPS_CHAT_SUB_TITLE
import com.joshtalks.joshskills.ui.group.GROUPS_CREATED_TIME
import com.joshtalks.joshskills.ui.group.GROUPS_CREATOR
import com.joshtalks.joshskills.ui.group.GROUPS_ID
import com.joshtalks.joshskills.ui.group.GROUPS_IMAGE
import com.joshtalks.joshskills.ui.group.GROUPS_STACK
import com.joshtalks.joshskills.ui.group.GROUPS_TITLE
import com.joshtalks.joshskills.ui.group.GroupChatFragment
import com.joshtalks.joshskills.ui.group.GroupListFragment
import com.joshtalks.joshskills.ui.group.GroupSearchFragment
import com.joshtalks.joshskills.ui.group.HAS_JOINED_GROUP
import com.joshtalks.joshskills.ui.group.IS_FROM_VOIP
import com.joshtalks.joshskills.ui.group.LIST_FRAGMENT
import com.joshtalks.joshskills.ui.group.SEARCH_FRAGMENT
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

private const val TAG = "JoshVoipGroupActivity"
class JoshVoipGroupActivity : BaseActivity() {

    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    val binding by lazy<ActivityJoshVoipGroupctivityBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_josh_voip_groupctivity)
    }

    override fun initViewBinding() {
        binding.vm = vm
        vm.isFromVoip.set(true)
        binding.executePendingBindings()
    }

    override fun onCreated() {
        openGroupListFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when(it.what) {
                ON_BACK_PRESSED -> onBackPressed()
                OPEN_GROUP -> {
                    if(supportFragmentManager.backStackEntryCount == 0)
                        startGroupCall(it.obj as? GroupItemData)
                    else
                        openGroupChat(it.obj as? GroupItemData)
                }
                SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
                SEARCH_GROUP -> openGroupSearchFragment()
            }
        }
    }

    private fun openGroupListFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, GroupListFragment(), LIST_FRAGMENT)
        }
    }

    private fun startGroupCall(groupItemData : GroupItemData?) {
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

    fun openCallingActivity(groupItemData : GroupItemData?) {
        // TODO: Might need to refactor
        val memberText = groupItemData?.getSubTitle() ?: "0"
        val memberCount = getMemberCount(memberText)
        if( memberCount == 0) {
            com.joshtalks.joshskills.core.showToast("Unknown Error Occurred")
            return
        } else if(memberCount == 1) {
            com.joshtalks.joshskills.core.showToast("You are the only member, Can't Initiate a Call")
            return
        }

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

    private fun openGroupSearchFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putBoolean(IS_FROM_VOIP, true)
            }
            val fragment = GroupSearchFragment().apply {
                arguments = bundle
            }
            replace(R.id.group_fragment_container, fragment , SEARCH_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openGroupChat(data : GroupItemData?) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putString(GROUPS_CREATED_TIME, data?.getCreatedTime())
                putString(GROUPS_CREATOR, data?.getCreator())
                putString(GROUPS_TITLE, data?.getTitle())
                putString(GROUPS_CHAT_SUB_TITLE, data?.getSubTitle())
                putString(GROUPS_IMAGE, data?.getImageUrl())
                putString(GROUPS_ID, data?.getUniqueId())
                data?.hasJoined()?.let { putBoolean(HAS_JOINED_GROUP, it) }
            }
            val fragment = GroupChatFragment()
            fragment.arguments = bundle
            replace(R.id.group_fragment_container, fragment, CHAT_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }
}