package com.joshtalks.joshskills.ui.fpp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityRecentCallBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.fpp.viewmodels.RecentCallViewModel
import kotlinx.coroutines.delay

val SENT_REQUEST = "send_request"
val IS_ALREADY_FPP = "is_already_fpp"
val ALREADY_FPP="already_fpp"
val REQUESTED = "requested"
val HAS_RECIEVED_REQUEST = "has_recieved_request"

class RecentCallActivity : WebRtcMiddlewareActivity() {
val HAS_RECIEVED_REQUEST="has_recieved_request"
class RecentCallActivity : WebRtcMiddlewareActivity(), AdapterCallback {
    private lateinit var binding: ActivityRecentCallBinding
    lateinit var recentCallAdapter: RecentCallsAdapter

    private val viewModel: RecentCallViewModel by lazy {
        ViewModelProvider(this).get(RecentCallViewModel::class.java)
    }
    var flag: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_recent_call)
        binding.handler = this
        setSupportActionBar(binding.toolbar)
        initView()
        addObservable()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getFavorites()
    }

    private fun initView() {
        var recyclerView: RecyclerView = binding.recentListRv
        recyclerView.layoutManager = LinearLayoutManager(applicationContext).apply {
            isSmoothScrollbarEnabled = true
        }
        recyclerView.setHasFixedSize(true)
        recentCallAdapter= RecentCallsAdapter(this,this,this,intent.getStringExtra(
            CONVERSATION_ID))
        recyclerView.adapter = recentCallAdapter
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
        viewModel.recentCallList.observe(this){
                if (it != null) {
                    recentCallAdapter.addItems(it.arrayList)
                }
            }

        viewModel.apiCallStatus.observe(this) {
            if(flag) {
                when (it) {
                    ApiCallStatus.SUCCESS -> {
                        FullScreenProgressDialog.hideProgressBar(this)
                        flag = false
                    }
                    ApiCallStatus.FAILED -> {
                        FullScreenProgressDialog.hideProgressBar(this)
                        this.finish()
                    }
                    ApiCallStatus.START ->
                        FullScreenProgressDialog.showProgressBar(this)
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
                val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.respond_request_alert_dialog, null)
                dialogBuilder.setView(dialogView)
                val alertDialog: AlertDialog = dialogBuilder.create()
                val width = AppObjectController.screenWidth * .9
                val height = ViewGroup.LayoutParams.WRAP_CONTENT
                alertDialog.show()
                alertDialog.window?.setLayout(width.toInt(), height)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogView.findViewById<TextView>(R.id.text).text =name + " has requested to be your favorite practice partner"
                dialogView.findViewById<MaterialTextView>(R.id.confirm_button).setOnClickListener {
                    if (mentorId != null) {
                        viewModel.confirmOrRejectFppRequest(mentorId, ISACCEPTED,"RECENT_CALL")
                        alertDialog.dismiss()
                    }
                }
                dialogView.findViewById<MaterialTextView>(R.id.not_now).setOnClickListener {
                    if (mentorId != null) {
                        viewModel.confirmOrRejectFppRequest(mentorId, ISREJECTED,"RECENT_CALL")
                        alertDialog.dismiss()
                    }
                }

            }

        }
    }
}