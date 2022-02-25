package com.joshtalks.joshskills.ui.fpp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.databinding.ActivityRecentCallBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.fpp.viewmodels.RecentCallViewModel

class RecentCallActivity : WebRtcMiddlewareActivity(), AdapterCallback {
    private lateinit var binding: ActivityRecentCallBinding
    lateinit var recentCallAdapter: RecentCallsAdapter
    var recyclerView: RecyclerView? = null
    var itemPosition: Int = 0
    var isFirstTime = true
    private val viewModel: RecentCallViewModel by lazy {
        ViewModelProvider(this).get(RecentCallViewModel::class.java)
    }
    var flag: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_recent_call)
        binding.handler = this
        recyclerView = binding.recentListRv

        setSupportActionBar(binding.toolbar)
        addObservable()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getFavorites()
    }

    private fun initView(recentCallList: ArrayList<RecentCall>?) {
        recyclerView?.layoutManager = LinearLayoutManager(applicationContext).apply {
            isSmoothScrollbarEnabled = true
        }
        recyclerView?.setHasFixedSize(true)
        recentCallAdapter = RecentCallsAdapter(
            this,
            this,
            this,
            intent.getStringExtra(CONVERSATION_ID),
            recentCallList
        )
        recyclerView?.adapter = recentCallAdapter
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
        viewModel.recentCallList.observe(this) {
            if (it != null) {
                if (isFirstTime) {
                    initView(it.arrayList)
                    isFirstTime = false
                } else {
                    recentCallAdapter.updateList(it.arrayList, recyclerView, itemPosition)
                }
            }
        }

        viewModel.apiCallStatus.observe(this) {
            if (flag) {
                when (it) {
                    ApiCallStatus.SUCCESS -> {
                        binding.progressBar.visibility = View.GONE
                        if (recentCallAdapter.itemCount == 0) {
                            binding.emptyCard.visibility = View.VISIBLE
                        }
                        flag = false
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

    }

    override fun onClickCallback(
        requestStatus: String?,
        mentorId: String?,
        position: Int,
        name: String?
    ) {
        itemPosition = position
        when (requestStatus) {
            SENT_REQUEST -> {
                if (mentorId != null) {
                    viewModel.sendFppRequest(mentorId)
                }
            }
            REQUESTED -> {
                if (mentorId != null) {
                    viewModel.deleteFppRequest(mentorId)
                }
            }
            HAS_RECIEVED_REQUEST -> {
                val dialogView = Dialog(this)
                dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)

                dialogView.setCancelable(true)
                dialogView.setContentView(R.layout.respond_request_alert_dialog)
                dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogView.show()

                val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.confirm_button)
                val btnNotNow = dialogView.findViewById<MaterialButton>(R.id.not_now)
                dialogView.findViewById<TextView>(R.id.text).text =
                    "$name has requested to be your favorite practice partner"
                btnConfirm
                    .setOnClickListener {
                        if (mentorId != null) {
                            viewModel.confirmOrRejectFppRequest(
                                mentorId,
                                IS_ACCEPTED,
                                RECENT_CALL
                            )
                            dialogView.dismiss()
                        }
                    }
                btnNotNow.setOnClickListener {
                    if (mentorId != null) {
                        viewModel.confirmOrRejectFppRequest(mentorId, IS_REJECTED, RECENT_CALL)
                        dialogView.dismiss()
                    }
                }

            }

        }
    }
}