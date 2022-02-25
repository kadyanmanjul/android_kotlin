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
import com.joshtalks.joshskills.databinding.ActivitySeeAllRequestsBinding
import com.joshtalks.joshskills.ui.fpp.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.fpp.adapters.SeeAllRequestsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.REQUESTS_SCREEN
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.fpp.viewmodels.SeeAllRequestsViewModel

class SeeAllRequestsActivity : AppCompatActivity(), AdapterCallback {
    lateinit var binding: ActivitySeeAllRequestsBinding
    lateinit var seeAllRequestsAdapter: SeeAllRequestsAdapter
    private val viewModel by lazy {
        ViewModelProvider(this).get(
            SeeAllRequestsViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_see_all_requests)
        binding.lifecycleOwner = this
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        addObservable()
    }

    override fun onStart() {
        super.onStart()
        getPendingRequests()
    }

    private fun getPendingRequests() {
        viewModel.getPendingRequestsList()
    }

    private fun addObservable() {
        viewModel.pendingRequestsList.observe(this) {
            initView(it.pendingRequestsList)
        }
        viewModel.apiCallStatus.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    if (seeAllRequestsAdapter.itemCount == 0) {
                        binding.fppNoRequests.visibility = View.VISIBLE
                    }
                }
                ApiCallStatus.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    this.finish()
                }
                ApiCallStatus.START ->
                    binding.progressBar.visibility = View.VISIBLE
            }
        }

    }


    private fun initView(pendingRequestsList: List<PendingRequestDetail>) {
        val recyclerView: RecyclerView = binding.recentListRv
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        seeAllRequestsAdapter = SeeAllRequestsAdapter(pendingRequestsList, this, this)
        recyclerView.adapter = seeAllRequestsAdapter
    }

    override fun onClickCallback(
        requestStatus: String?,
        mentorId: String?,
        position: Int,
        name: String?
    ) {
        if (requestStatus != null) {
            if (mentorId != null) {
                viewModel.confirmOrRejectFppRequest(mentorId, requestStatus, REQUESTS_SCREEN)
            }
        }
    }
}