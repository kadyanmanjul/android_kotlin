package com.joshtalks.joshskills.ui.group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.databinding.ActivityJoshGroupBinding

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

    }

    override fun initViewState() {
        liveData.observe(this) {
            when(it.what) {
                ON_BACK_PRESSED -> {
                    showToast("Back Pressed")
                }
            }
        }
    }
}