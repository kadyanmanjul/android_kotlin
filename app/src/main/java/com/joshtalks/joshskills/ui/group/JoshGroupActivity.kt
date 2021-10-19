package com.joshtalks.joshskills.ui.group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flurry.sdk.it
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.OPEN_NEW_GROUP_FRAGMENT
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JoshGroupActivity : BaseActivity() {
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
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.group_fragment_container, GroupListFragment(), "NEW_GROUP_FRAGMENT")
        }
    }

    override fun initViewState() {
        liveData.observe(this) {
            when(it.what) {
                ON_BACK_PRESSED -> {
                    showToast("Back Pressed ${supportFragmentManager.backStackEntryCount}")
                    if(supportFragmentManager.backStackEntryCount == 1) {
                        supportFragmentManager.popBackStack()
                    }
                }
                OPEN_GROUP -> {
                    val groupData = it.obj as? GroupItemData
                    showToast("Open Group ${groupData?.getUniqueId()}")
                    if(groupData?.getUniqueId() == 6) {
                        // TODO: Use Fragment for Group List
                        supportFragmentManager.commit {
                            setReorderingAllowed(true)
                            add(R.id.group_fragment_container, GroupChatFragment(), "NEW_GROUP_FRAGMENT")
                            addToBackStack("GROUPS_STACK")
                        }
                    }
                }
                OPEN_NEW_GROUP_FRAGMENT -> {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.group_fragment_container, NewGroupFragment(), "NEW_GROUP_FRAGMENT")
                        addToBackStack("GROUPS_STACK")
                    }
                }
            }
        }
    }

    fun retry() {
        vm.adapter.refresh()
    }
}