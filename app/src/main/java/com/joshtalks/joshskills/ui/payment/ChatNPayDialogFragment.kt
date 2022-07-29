package com.joshtalks.joshskills.ui.payment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.freshchat.consumer.sdk.Freshchat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.FRESH_CHAT_UNREAD_MESSAGES
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentChatNPayBinding


class ChatNPayDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChatNPayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_chat_n_pay,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        binding.chatPay.setOnClickListener { openFreshChat() }
        binding.close.setOnClickListener { dismissAndCloseActivity() }
    }

    private fun dismissAndCloseActivity() {
        dismiss()
        activity?.finish()
    }

    private fun openFreshChat() {
        try {
            AppAnalytics.create(AnalyticsEvent.HELP_CHAT.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
            Freshchat.showConversations(requireContext())
            PrefManager.put(FRESH_CHAT_UNREAD_MESSAGES, 0)
            activity?.finish()
        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.something_went_wrong))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ChatNPayDialogFragment()
    }
}
