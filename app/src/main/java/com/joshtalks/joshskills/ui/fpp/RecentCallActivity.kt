package com.joshtalks.joshskills.ui.fpp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityRecentCallBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.fpp.viewmodels.RecentCallViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

val SENT_REQUEST = "send_request"
val IS_ALREADY_FPP = "is_already_fpp"
val REQUESTED = "requested"
val HAS_RECIEVED_REQUEST="has_recieved_request"
class RecentCallActivity : WebRtcMiddlewareActivity(), AdapterCallback {
    private lateinit var binding: ActivityRecentCallBinding
    private val recentCallAdapter by lazy { RecentCallsAdapter(this,this) }

    private val viewModel: RecentCallViewModel by lazy {
        ViewModelProvider(this).get(RecentCallViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_recent_call)
        binding.handler = this
        setSupportActionBar(binding.toolbar)
        initView()
        addObservable()
        viewModel.getFavorites()
    }

    private fun initView() {

        binding.recentListRv.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(applicationContext).apply {
                isSmoothScrollbarEnabled = true
            }
            adapter = recentCallAdapter
        }
    }

    companion object {
        fun openRecentCallActivity(activity: Activity, conversationId: String) {
            Intent(activity, RecentCallActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
            }.also {
                activity.startActivity(it)
            }
        }
    }

    private fun addObservable() {
        lifecycleScope.launchWhenStarted {
            viewModel.recentCallList.collect {
                if (it == null) {
                    return@collect
                }
                delay(350)
                recentCallAdapter.addItems(it.arrayList)
                binding.progressBar.visibility = View.GONE
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.apiCallStatus.collect {
                binding.progressBar.visibility = View.GONE
                if (recentCallAdapter.itemCount == 0) {
                    showToast("You can add partners to this list by ")
                }
            }
        }
    }
    override fun onClickCallback(requestStatus: String?, mentorId: String?) {
        when(requestStatus){
            SENT_REQUEST->{
                if (mentorId != null) {
                    viewModel.sendFppRequest(mentorId)
                }
            }
            REQUESTED->{
                if (mentorId != null) {
                    viewModel.deleteFppRequest(mentorId)
                }
            }
            HAS_RECIEVED_REQUEST->{

            }

        }
    }
}