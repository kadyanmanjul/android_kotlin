package com.joshtalks.joshskills.fpp

import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.base.BaseActivity
import com.joshtalks.joshskills.fpp.adapters.SeeAllRequestsAdapter
import com.joshtalks.joshskills.fpp.constants.FAVOURITE_REQUEST
import com.joshtalks.joshskills.fpp.constants.FPP_SEE_ALL_BACK_PRESSED
import com.joshtalks.joshskills.fpp.constants.FPP_OPEN_USER_PROFILE
import com.joshtalks.joshskills.fpp.viewmodels.SeeAllRequestsViewModel
import com.joshtalks.joshskills.common.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.fpp.databinding.ActivitySeeAllRequestsBinding
import java.lang.Exception

class SeeAllRequestsActivity : BaseActivity() {

    val binding by lazy<ActivitySeeAllRequestsBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_see_all_requests)
    }
    lateinit var seeAllRequestsAdapter: SeeAllRequestsAdapter

    val viewModel by lazy {
        ViewModelProvider(this)[SeeAllRequestsViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        viewModel.getPendingRequestsList()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                FPP_SEE_ALL_BACK_PRESSED -> popBackStack()
                FPP_OPEN_USER_PROFILE -> openUserProfile(it.obj.toString())
            }
        }
    }

    private fun openUserProfile(senderMentorId: String) {
        UserProfileActivity.startUserProfileActivity(
            this,
            senderMentorId,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            previousPage = FAVOURITE_REQUEST,
            conversationId = null
        )
    }

    private fun popBackStack() {
        try {
            if (supportFragmentManager.backStackEntryCount>0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressed()
            }
        }catch (ex: Exception){
            ex.printStackTrace()
        }
    }
}