package com.joshtalks.joshskills.common.ui.invite_call

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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.BaseActivity
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.databinding.ActivityInviteFriendBinding
import com.joshtalks.joshskills.common.repository.local.entity.PhonebookContact
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.util.DeepLinkUtil
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException

class InviteFriendActivity : BaseActivity(), ContactsAdapter.OnContactClickListener {
    private val binding: ActivityInviteFriendBinding by lazy {
        ActivityInviteFriendBinding.inflate(layoutInflater)
    }
    private val viewModel: InviteFriendViewModel by lazy {
        ViewModelProvider(this).get(InviteFriendViewModel::class.java)
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
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        binding.searchView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            binding.inviteInfo.isVisible = hasFocus.not()
        }
    }

    override fun onContactClick(contact: PhonebookContact) {
        if (isPhoneNumberValid(contact.phoneNumber).not()) return
        viewModel.isLoading.set(true)
        com.joshtalks.joshskills.common.util.DeepLinkUtil(this)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : com.joshtalks.joshskills.common.util.DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriend(contact, deepLink)
                }
            })
            .build()
    }

    override fun onStart() {
        super.onStart()
        PermissionUtils.isReadContactPermissionEnabled(this).also {
            viewModel.isContactsPermissionEnabled.set(it)
            if (it)
                viewModel.readContacts()
            else
                requestContactsPermission()
        }
    }

    fun requestContactsPermission(v: View? = null) {
        PermissionUtils.requestReadContactPermission(this, object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                viewModel.isContactsPermissionEnabled.set(true)
                viewModel.readContacts()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                PermissionUtils.permissionPermanentlyDeniedDialog(
                    activity = this@InviteFriendActivity,
                    message = R.string.permission_denied_contacts,
                    onPermissionDenied = {
                        viewModel.isContactsPermissionEnabled.set(false)
                    }
                )
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?
            ) {
                p1?.continuePermissionRequest()
            }
        })
    }

    fun isPhoneNumberValid(phoneNumber: String): Boolean {
        return try {
            if (phoneNumber.isEmpty())
                throw IOException("Phone number is empty")
            val regex = Regex("^[6-9][0-9]{9}$")
            if (!regex.matches(phoneNumber.substring(3)))
                throw IOException("Phone number is invalid")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            MaterialAlertDialogBuilder(this)
                .setTitle("Unable to call friend!")
                .setMessage(
                    e.message
                )
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            false
        }
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