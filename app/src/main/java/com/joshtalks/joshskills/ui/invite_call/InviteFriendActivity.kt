package com.joshtalks.joshskills.ui.invite_call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityInviteFriendBinding
import com.joshtalks.joshskills.repository.local.entity.PhonebookContact
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.DeepLinkUtil

class InviteFriendActivity : BaseActivity(), ContactsAdapter.OnContactClickListener {
    private val viewModel: InviteFriendViewModel by lazy {
        ViewModelProvider(this).get(InviteFriendViewModel::class.java)
    }
    private val binding: ActivityInviteFriendBinding by lazy {
        ActivityInviteFriendBinding.inflate(layoutInflater)
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            Intent(context, InviteFriendActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        findViewById<TextView>(R.id.text_message_title).text = getString(R.string.call_a_friend)
        findViewById<ImageView>(R.id.iv_help).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                MaterialAlertDialogBuilder(this@InviteFriendActivity)
                    .setMessage(
                        getString(R.string.invite_friend_call_info)
                    )
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        findViewById<View>(R.id.iv_back).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.handler = this
        binding.searchView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            binding.inviteInfo.isVisible = hasFocus.not()
        }
    }

    override fun onContactClick(contact: PhonebookContact) {
        viewModel.isLoading.set(true)
        DeepLinkUtil(this)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriend(contact, deepLink)
                }
            })
            .build()
    }

    fun inviteFriend(contact: PhonebookContact, deepLink: String) {
        viewModel.inviteFriend(
            contact = contact,
            deepLink = deepLink,
            onSuccess = {
                MaterialAlertDialogBuilder(this)
                    .setMessage(
                        getString(R.string.invite_friend_link_sent)
                    )
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            },
            onError = {
                Snackbar.make(
                    binding.root,
                    "An error occurred",
                    Snackbar.LENGTH_SHORT
                )
                    .setAction("Retry") {
                        inviteFriend(contact, deepLink)
                    }.show()
            }
        )

    }
}