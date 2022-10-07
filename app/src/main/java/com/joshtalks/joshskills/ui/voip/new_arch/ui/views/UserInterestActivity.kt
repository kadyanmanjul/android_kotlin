package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.CLOSE_INTEREST_ACTIVITY
import com.joshtalks.joshskills.constants.START_USER_INTEREST_FRAGMENT
import com.joshtalks.joshskills.databinding.ActivityUserInterestBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import kotlinx.coroutines.sync.Mutex

class UserInterestActivity : BaseActivity() {

    val binding by lazy<ActivityUserInterestBinding> { DataBindingUtil.setContentView(this,R.layout.activity_user_interest) }

    val viewModel by lazy { ViewModelProvider(this)[CallInterestViewModel::class.java] }

    private val backPressMutex = Mutex(false)

    override fun onCreated() {
        addEnglishLevelFragment()
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }
    override fun initViewState() {
        event.observe(this){
            when(it.what){
                START_USER_INTEREST_FRAGMENT -> addInterestFragment()

                CLOSE_INTEREST_ACTIVITY -> finish()
            }
        }
    }

    private fun addEnglishLevelFragment(){
        supportFragmentManager.commit {
            replace(R.id.container_Interests,UserEnglishLevelFragment(),"User English Level Fragment")
        }
    }

    private fun addInterestFragment(){
        supportFragmentManager.commit {
            replace(R.id.container_Interests,CallInterestFragment(),"Interest Fragment")
                .addToBackStack("User English Level Fragment")
        }
    }

    override fun onBackPressed() {
        when(supportFragmentManager.findFragmentById(R.id.container_Interests)){
            is CallInterestFragment ->{
                backPressMutex.onMultipleBackPress {
                    super.onBackPressed()
                }
            }

            is UserEnglishLevelFragment ->{
                backPressMutex.onMultipleBackPress {
                    super.onBackPressed()
                }
            }

            else->{
                super.onBackPressed()
            }
        }

    }
}