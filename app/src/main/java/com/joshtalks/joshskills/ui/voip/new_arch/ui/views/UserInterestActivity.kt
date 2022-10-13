package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_CALL_DURATION
import com.joshtalks.joshskills.constants.CLOSE_INTEREST_ACTIVITY
import com.joshtalks.joshskills.constants.SHOW_BUY_POPUP_FT
import com.joshtalks.joshskills.constants.START_USER_INTEREST_FRAGMENT
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityUserInterestBinding
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.lesson.PurchaseDialog
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

                SHOW_BUY_POPUP_FT -> {
                    showPurchaseDialog(prefManager.getLong(PREF_KEY_LAST_CALL_DURATION,0))
                    //TODO: add some mechanism to close activity after dialog is dismissed
                }
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
                replace(R.id.container_Interests,CallInterestFragment(isEditCall),"Interest Fragment")
            }else{
                replace(R.id.container_Interests,CallInterestFragment(isEditCall),"Interest Fragment")
                    .addToBackStack("User English Level Fragment")
            }
        }
    }

    private fun showPurchaseDialog(duration: Long) {
        CoroutineScope(Dispatchers.IO + VoipPref.coroutineExceptionHandler).launch {
            try {
                val resp =
                    AppObjectController.commonNetworkService.getCoursePopUpData(
                        courseId = PrefManager.getStringValue(CURRENT_COURSE_ID),
                        popupName = PurchasePopupType.SPEAKING_COMPLETED.name,
                        callCount = PrefManager.getIntValue(FT_CALLS_LEFT),
                        callDuration = duration
                    )
                resp.body()?.let {
                    if (it.couponCode != null && it.couponExpiryTime != null)
                        PrefManager.put(COUPON_EXPIRY_TIME, it.couponExpiryTime.time)
                    PurchaseDialog.newInstance(it).show(supportFragmentManager,"PurchaseDialog")
                }
            } catch (ex: Exception) {
                Log.d("sagar", "showPurchaseDialog: ${ex.message}")
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