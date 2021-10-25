package com.joshtalks.joshskills.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.github.dhaval2404.imagepicker.ImagePicker
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_CALLING_ACTIVITY
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.OPEN_IMAGE_CHOOSER
import com.joshtalks.joshskills.constants.OPEN_NEW_GROUP
import com.joshtalks.joshskills.constants.SEARCH_GROUP
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.userprofile.UserPicChooserFragment
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import kotlinx.coroutines.flow.collectLatest
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
            when(it.what) {
                ON_BACK_PRESSED -> popBackStack()
                OPEN_GROUP -> openGroupChat(it.obj as? GroupItemData)
                OPEN_NEW_GROUP -> openNewGroupFragment()
                SEARCH_GROUP -> openGroupSearchFragment()
                OPEN_IMAGE_CHOOSER -> openImageChooser()
                OPEN_CALLING_ACTIVITY -> openCallingActivity(it.data)
                SHOULD_REFRESH_GROUP_LIST -> vm.shouldRefreshGroupList = true
            }
        }
    }

    fun openCallingActivity(bundle: Bundle) {
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

    private fun openNewGroupFragment(/*view: View?*/) {
        vm.addingNewGroup.set(false)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.group_fragment_container, NewGroupFragment(), ADD_GROUP_FRAGMENT)
            addToBackStack(GROUPS_STACK)
        }
    }

    private fun popBackStack() {
        if(supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
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
}