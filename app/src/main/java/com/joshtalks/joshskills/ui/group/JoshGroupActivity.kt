package com.joshtalks.joshskills.ui.group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flurry.sdk.it
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class JoshGroupActivity : BaseActivity() {
    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    val adapter by lazy {
        GroupAdapter(GroupItemComparator)
    }

    val binding by lazy<ActivityJoshGroupBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_josh_group)
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.groupRv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.groupRv.setHasFixedSize(false)
        binding.groupRv.adapter = adapter
        adapter.setListener(vm.onItemClick)
        binding.executePendingBindings()
    }

    override fun onCreated() {

    }

    override fun initViewState() {
        liveData.observe(this) {
            when(it.what) {
                ON_BACK_PRESSED -> {
                    showToast("Back Pressed")
                }
                OPEN_GROUP -> {
                    showToast("Open Group ${(it.obj as? GroupItemData)?.getUniqueId()}")
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            vm.flow.distinctUntilChanged().collectLatest {
                adapter.submitData(it)
            }
        }
    }
}