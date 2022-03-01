package com.joshtalks.joshskills.ui.group

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider

import com.afollestad.materialdialogs.MaterialDialog
import com.github.dhaval2404.imagepicker.ImagePicker

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.userprofile.fragments.UserPicChooserFragment
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity

import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import timber.log.Timber

private const val TAG = "JoshGroupActivity"

class JoshGroupActivity : BaseGroupActivity() {
    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    val binding by lazy<ActivityJoshGroupBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_josh_group)
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun onCreated() {
        openGroupListFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                ON_BACK_PRESSED -> popBackStack()
                OPEN_GROUP -> openGroupChat(it.obj as? GroupItemData)
                OPEN_NEW_GROUP -> openNewGroupFragment()
                OPEN_GROUP_INFO -> openGroupInfoFragment()
                EDIT_GROUP_INFO -> openEditGroupInfo(it.data)
                SEARCH_GROUP -> openGroupSearchFragment()
                OPEN_IMAGE_CHOOSER -> openImageChooser()
                OPEN_CALLING_ACTIVITY -> startGroupCall(it.data)
                SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
                REMOVE_GROUP_AND_CLOSE -> removeGroupFromDb(it.obj as String)
                OPEN_PROFILE_PAGE -> openProfileActivity(it.obj as String)
                SHOW_PROGRESS_BAR -> showProgressDialog(it.obj as String)
                DISMISS_PROGRESS_BAR -> dismissProgressDialog()
                REFRESH_GRP_LIST_HIDE_INFO -> {
                    setNewGroupVisibility(it.data)
                    vm.setGroupsCount()
                }
            }
        }
    }

    // TODO: Need to refactor
    private fun startGroupCall(data: Bundle) {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            openCallingActivity(data)
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                this@JoshGroupActivity,
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            openCallingActivity(data)
                            return
                        } else {
                            MaterialDialog(this@JoshGroupActivity).show {
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

    fun openCallingActivity(bundle: Bundle) {
        GroupAnalytics.push(GroupAnalytics.Event.CALL_PRACTICE_PARTNER_FROM_GROUP, bundle.getString(GROUPS_ID) ?: "")
        val intent = SearchingUserActivity.startUserForPractiseOnPhoneActivity(
            this,
            courseId = "151",
            topicId = 5,
            groupId = bundle.getString(GROUPS_ID),
            isGroupCallCall = true,
            topicName = "Group Call",
            groupName = bundle.getString(GROUPS_TITLE),
            favoriteUserCall = false
        )
        startActivity(intent)
    }

    private fun openGroupListFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, GroupListFragment(), LIST_FRAGMENT)
        }
    }

    private fun openGroupSearchFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putString(CONVERSATION_ID, vm.conversationId)
            }
            val fragment = GroupSearchFragment().apply {
                arguments = bundle
            }
            vm.openedGroupId = null
            replace(R.id.group_fragment_container, fragment, SEARCH_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openGroupChat(data: GroupItemData?) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putString(GROUPS_CREATED_TIME, data?.getCreatedTime())
                putString(GROUPS_CREATOR, data?.getCreator())
                putString(GROUPS_TITLE, data?.getTitle())
                putString(GROUPS_IMAGE, data?.getImageUrl())
                putString(GROUPS_CHAT_SUB_TITLE, data?.getSubTitle())
                putString(GROUPS_ID, data?.getUniqueId())
                putString(CONVERSATION_ID, vm.conversationId)
                putString(ADMIN_ID, data?.getCreatorId())
                data?.hasJoined()?.let {
                    if (it) {
                        putString(GROUPS_CHAT_SUB_TITLE, "tap here for group info")
                        putInt(GROUP_CHAT_UNREAD, Integer.valueOf(data.getUnreadMsgCount()))
                        GroupAnalytics.push(GroupAnalytics.Event.OPEN_GROUP, data.getUniqueId())
                    }
                    putBoolean(HAS_JOINED_GROUP, it)
                }
            }

            val fragment = GroupChatFragment()
            fragment.arguments = bundle
            replace(R.id.group_fragment_container, fragment, CHAT_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openGroupInfoFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)

            val fragment = GroupInfoFragment()
            add(R.id.group_fragment_container, fragment, GROUP_INFO_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openEditGroupInfo(data: Bundle?) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putBoolean(IS_FROM_GROUP_INFO, true)
                putString(GROUPS_TITLE, data?.getString(GROUPS_TITLE))
                putString(GROUPS_IMAGE, data?.getString(GROUPS_IMAGE))
                putString(GROUPS_ID, data?.getString(GROUPS_ID))
            }
            val fragment = NewGroupFragment()
            fragment.arguments = bundle

            replace(R.id.group_fragment_container, fragment, EDIT_GROUP_INFO_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun openNewGroupFragment() {
        vm.addingNewGroup.set(false)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle().apply {
                putBoolean(IS_FROM_GROUP_INFO, false)
            }
            val fragment = NewGroupFragment()
            fragment.arguments = bundle

            vm.openedGroupId = null
            replace(R.id.group_fragment_container, fragment, ADD_GROUP_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
        GroupAnalytics.push(GroupAnalytics.Event.CREATE_GROUP)
    }

    private fun setNewGroupVisibility(data: Bundle) {
        vm.hasGroupData.set(data.getBoolean(SHOW_NEW_INFO))
        vm.hasGroupData.notifyChange()
        vm.shouldRefreshGroupList = true
    }

    private fun popBackStack() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            try {
                supportFragmentManager.popBackStackImmediate()
            } catch ( ex:Exception){
                ex.printStackTrace()
            }
        } else
            onBackPressed()
    }

    private fun openImageChooser() {
        UserPicChooserFragment.showDialog(
            supportFragmentManager,
            true,
            isFromRegistration = false,
            isFromGroup = true
        )
    }

    private fun removeGroupFromDb(groupId: String) {
        if (groupId == vm.openedGroupId)
            while (supportFragmentManager.backStackEntryCount > 0)
                onBackPressed()
        CoroutineScope(Dispatchers.IO).launch {
            val groupName = vm.repository.getGroupName(groupId)
            vm.repository.leaveGroupFromLocal(groupId)
            withContext(Dispatchers.Main) {
                showRemovedAlert(groupName)
            }
        }
    }

    override fun getConversationId(): String? {
        vm.conversationId = intent.getStringExtra(CONVERSATION_ID) ?: ""
        return vm.conversationId
    }

    fun openProfileActivity(mentorId: String) {
        UserProfileActivity.startUserProfileActivity(
            activity = this,
            mentorId = mentorId,
            flags = arrayOf(),
            intervalType =  null,
            previousPage = this.javaClass.name,
            conversationId = null
        )
    }

    fun showRemovedAlert(groupName: String) {
        val builder = AlertDialog.Builder(this)
        val dialog: AlertDialog = builder
            .setMessage("You have been removed from \"$groupName\" group")
            .setPositiveButton("Ok") { dialog, id ->
                dialog.cancel()
            }
            .create()

        dialog.setCancelable(false)
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).let {
            it.setTypeface(null, Typeface.BOLD)
            it.setTextColor(Color.parseColor("#107BE5"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val url = data?.data?.path ?: EMPTY
            if (url.isNotBlank()) {
                vm.showImageThumb(url)
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Timber.e(ImagePicker.getError(data))
            showToast(ImagePicker.getError(data))
        }
    }

    override fun setIntentExtras() {}

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            vm.deleteExtraMessages()
        }
    }
}