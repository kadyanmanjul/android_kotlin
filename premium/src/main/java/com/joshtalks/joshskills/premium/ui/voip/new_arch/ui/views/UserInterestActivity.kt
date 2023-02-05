package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views

import android.content.Context
import android.content.SharedPreferences
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseActivity
import com.joshtalks.joshskills.premium.constants.CLOSE_INTEREST_ACTIVITY
import com.joshtalks.joshskills.premium.constants.START_USER_INTEREST_FRAGMENT
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.databinding.ActivityUserInterestBinding
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import kotlinx.coroutines.sync.Mutex

class UserInterestActivity : BaseActivity() {

    val binding by lazy<ActivityUserInterestBinding> { DataBindingUtil.setContentView(this,R.layout.activity_user_interest) }

    val viewModel by lazy { ViewModelProvider(this)[CallInterestViewModel::class.java] }

    val prefManager: SharedPreferences by lazy { getSharedPreferences(getString(R.string.voip_shared_pref_file_name), Context.MODE_PRIVATE) }

    private var isEditCall = false

    private val backPressMutex = Mutex(false)

    override fun onCreated() {
        isEditCall = intent.getBooleanExtra("isEditCall",false)

        if (isEditCall){    // if opened from menu, to edit, only show interest fragment
            addInterestFragment()
        }else{  // if normal call then show english level frag first
            addEnglishLevelFragment()
        }
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
            if (isEditCall){
                replace(R.id.container_Interests,CallInterestFragment.newInstance(isEditCall),"Interest Fragment")
            }else{
                replace(R.id.container_Interests,CallInterestFragment.newInstance(isEditCall),"Interest Fragment")
                    .addToBackStack("User English Level Fragment")
            }
        }
    }


    override fun onBackPressed() {
        when(supportFragmentManager.findFragmentById(R.id.container_Interests)){
            is CallInterestFragment ->{
                backPressMutex.onMultipleBackPress {
                    viewModel.saveImpression(INTEREST_FORM_BACKPRESSED)
                    super.onBackPressed()
                }
            }

            is UserEnglishLevelFragment ->{
                backPressMutex.onMultipleBackPress {
                    viewModel.saveImpression(INTEREST_FORM_BACKPRESSED)
                    super.onBackPressed()
                }
            }

            else->{
                viewModel.saveImpression(INTEREST_FORM_BACKPRESSED)
                super.onBackPressed()
            }
        }

    }
}