package com.joshtalks.joshskills.ui.fpp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityRecentCallBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.fpp.viewmodels.RecentCallViewModel
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class RecentCallActivity : BaseFppActivity() {

    val binding by lazy<ActivityRecentCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_recent_call)
    }

    private val viewModel: RecentCallViewModel by lazy {
        ViewModelProvider(this).get(RecentCallViewModel::class.java)
    }

    var conversationId1 = EMPTY
    lateinit var recentCallAdapter: RecentCallsAdapter

    override fun setIntentExtras() {
        conversationId1 = intent.extras?.get(CONVERSATION_ID) as String
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        viewModel.getRecentCall()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                FPP_RECENT_CALL_ON_BACK_PRESS -> popBackStack()
                RECENT_OPEN_USER_PROFILE -> openUserProfileActivity(it.obj.toString())
                SCROLL_TO_POSITION -> {
                    binding.recentListRv.layoutManager?.scrollToPosition(it.obj as Int)
                }
                RECENT_CALL_USER_BLOCK -> {
                    onUserBlock(it.obj as RecentCall)
                }
                RECENT_CALL_HAS_RECIEVED_REQUESTED -> {
                    onRecentCallHasRequest(it.obj as RecentCall)
                }
            }
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

    private fun popBackStack() {
        onBackPressed()
    }

    fun openUserProfileActivity(id: String) {
        UserProfileActivity.startUserProfileActivity(
            this,
            id,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            RECENT_CALL,
            conversationId = conversationId1
        )
    }

    fun onRecentCallHasRequest(recentCall: RecentCall) {
        val dialogView = showCustomDialog(R.layout.respond_request_alert_dialog)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.confirm_button)
        val btnNotNow = dialogView.findViewById<MaterialButton>(R.id.not_now)
        dialogView.findViewById<TextView>(R.id.text).text =
            "${recentCall.firstName} has requested to be your favorite practice partner"
        btnConfirm
            .setOnClickListener {
                viewModel.confirmOrRejectFppRequest(
                    recentCall.receiverMentorId,
                    IS_ACCEPTED,
                    RECENT_CALL
                )
                dialogView.dismiss()
            }
        btnNotNow.setOnClickListener {
            viewModel.confirmOrRejectFppRequest(recentCall.receiverMentorId, IS_REJECTED, RECENT_CALL)
            dialogView.dismiss()
        }
    }

    fun onUserBlock(recentCall: RecentCall) {
        val dialogView = showCustomDialog(R.layout.block_user_alert_dialog)
        val btnConfirm = dialogView.findViewById<AppCompatTextView>(R.id.yes_button)
        val btnNotNow = dialogView.findViewById<AppCompatTextView>(R.id.not_now)
        dialogView.findViewById<TextView>(R.id.text).text = "Block ${recentCall.firstName}"
        btnConfirm
            .setOnClickListener {
                viewModel.blockUser(recentCall.receiverMentorId)
                dialogView.dismiss()
            }
        btnNotNow.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun showCustomDialog(view: Int): Dialog {
        val dialogView = Dialog(this)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.setCancelable(true)
        dialogView.setContentView(view)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.show()
        return dialogView
    }

}