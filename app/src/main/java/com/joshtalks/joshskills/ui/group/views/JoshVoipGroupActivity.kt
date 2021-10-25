package com.joshtalks.joshskills.ui.group.views

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.dhaval2404.imagepicker.ImagePicker
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.NO_GROUP_AVAILABLE
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_CALLING_ACTIVITY
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.OPEN_IMAGE_CHOOSER
import com.joshtalks.joshskills.constants.OPEN_NEW_GROUP
import com.joshtalks.joshskills.constants.SEARCH_GROUP
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding
import com.joshtalks.joshskills.databinding.ActivityJoshVoipGroupctivityBinding
import com.joshtalks.joshskills.ui.group.ADD_GROUP_FRAGMENT
import com.joshtalks.joshskills.ui.group.CHAT_FRAGMENT
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
import com.joshtalks.joshskills.ui.group.LIST_FRAGMENT
import com.joshtalks.joshskills.ui.group.NewGroupFragment
import com.joshtalks.joshskills.ui.group.SEARCH_FRAGMENT
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.userprofile.UserPicChooserFragment
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

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
                ON_BACK_PRESSED -> popBackStack()
                OPEN_GROUP -> {
                    if(supportFragmentManager.backStackEntryCount == 0)
                        openCallingActivity(it.obj as? GroupItemData)
                    else
                        openGroupChat(it.obj as? GroupItemData)
                }
                SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
                NO_GROUP_AVAILABLE -> openGroupSearchFragment()
            }
        }
    }

    private fun openGroupListFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, GroupListFragment(), LIST_FRAGMENT)
        }
    }

    fun openCallingActivity(groupItemData: GroupItemData?) {
        val intent = SearchingUserActivity.startUserForPractiseOnPhoneActivity(
            this,
            courseId = "151",
            topicId = 5,
            groupId = groupItemData?.getUniqueId(),
            isGroupCallCall = true,
            topicName = "Group Call",
            favoriteUserCall = false
        )
        startActivity(intent)
        finish()
    }

    private fun openGroupSearchFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, GroupSearchFragment(), SEARCH_FRAGMENT)
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

    private fun popBackStack() {
        if(supportFragmentManager.backStackEntryCount == 1) {
            if(vm.shouldRefreshGroupList)
                supportFragmentManager.popBackStack()
            else
                finish()
        } else
            onBackPressed()
    }
}