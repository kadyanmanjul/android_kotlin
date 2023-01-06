package com.joshtalks.joshskills.fpp

import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.base.BaseActivity
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.fpp.adapters.SeeAllRequestsAdapter
import com.joshtalks.joshskills.fpp.constants.FAVOURITE_REQUEST
import com.joshtalks.joshskills.fpp.constants.FPP_SEE_ALL_BACK_PRESSED
import com.joshtalks.joshskills.fpp.constants.FPP_OPEN_USER_PROFILE
import com.joshtalks.joshskills.fpp.viewmodels.SeeAllRequestsViewModel
import com.joshtalks.joshskills.fpp.databinding.ActivitySeeAllRequestsBinding
import java.lang.Exception

class SeeAllRequestsActivity : BaseActivity() {

    val binding by lazy<ActivitySeeAllRequestsBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_see_all_requests)
    }
    lateinit var seeAllRequestsAdapter: SeeAllRequestsAdapter
    private lateinit var navigator: Navigator

    val viewModel by lazy {
        ViewModelProvider(this)[SeeAllRequestsViewModel::class.java]
    }

    override fun getArguments() {
        navigator = intent.getSerializableExtra(NAVIGATOR) as Navigator
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
        navigator.with(this).navigate(object : UserProfileContract {
            override val mentorId = senderMentorId
            override val previousPage = FAVOURITE_REQUEST
            override val flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            override val navigator = this@SeeAllRequestsActivity.navigator
        })
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

    companion object {
        fun openAllRequestsActivity(contract: AllRequestsContract, context: Context) {
            context.startActivity(
                Intent(context, SeeAllRequestsActivity::class.java).apply {
                    putExtra(NAVIGATOR, contract.navigator)
                    contract.flags.forEach { addFlags(it) }
                }
            )
        }
    }
}