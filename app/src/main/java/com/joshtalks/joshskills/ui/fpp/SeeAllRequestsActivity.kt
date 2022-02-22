package com.joshtalks.joshskills.ui.fpp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivitySeeAllRequestsBinding
import com.joshtalks.joshskills.ui.fpp.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.fpp.adapters.SeeAllRequestsAdapter
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.fpp.viewmodels.SeeAllRequestsViewModel
val ISACCEPTED ="is_accepted"
val ISREJECTED = "is_rejected"
class SeeAllRequestsActivity : AppCompatActivity() ,AdapterCallback{
    lateinit var binding: ActivitySeeAllRequestsBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(
            SeeAllRequestsViewModel::class.java
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_see_all_requests)
        binding.lifecycleOwner = this
        binding.ivBack.setOnClickListener{
            onBackPressed()
        }
        addObserver()
    }

    override fun onStart() {
        super.onStart()
        getPendingRequests()
    }

    private fun getPendingRequests() {
       viewModel.getPendingRequestsList()
    }

    private fun addObserver() {
        viewModel.apiCallStatus.observe(this) {
            when(it){
                ApiCallStatus.SUCCESS->
                    FullScreenProgressDialog.hideProgressBar(this)
                ApiCallStatus.FAILED-> {
                    FullScreenProgressDialog.hideProgressBar(this)
                    this.finish()
                }
                ApiCallStatus.START->
                    FullScreenProgressDialog.showProgressBar(this)
            }
        }
        viewModel.pendingRequestsList.observe(this){

            initView(it.pendingRequestsList)
        }
    }

    private fun initView(pendingRequestsList: List<PendingRequestDetail>) {
        if(pendingRequestsList.isNullOrEmpty()){
        }else {
            var recyclerView: RecyclerView = binding.recentListRv
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = SeeAllRequestsAdapter(pendingRequestsList, this, this)
        }
    }

    override fun onClickCallback(
        requestStatus: String?,
        mentorId: String?,
        position: Int,
        name: String?
    ) {
        if (requestStatus != null) {
            if (mentorId != null) {
                viewModel.confirmOrRejectFppRequest(mentorId,requestStatus,"REQUESTS_SCREEN")
            }
        }
    }
}