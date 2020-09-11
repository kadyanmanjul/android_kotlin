package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.interfaces.OnSelectVerificationMethodListener
import com.joshtalks.joshskills.databinding.ReminderBottomSheetLayoutBinding
import com.joshtalks.joshskills.ui.inbox.InboxActivity

class ReminderBottomSheet : BottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ReminderBottomSheet()
    }

    private lateinit var binding: ReminderBottomSheetLayoutBinding
    private var listener: OnSelectVerificationMethodListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragment?.run {
            listener = this as OnSelectVerificationMethodListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.reminder_bottom_sheet_layout,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        binding.textView1.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_BOTTOM_POPUP_DESCRIPTION)

        return binding.root
    }

    fun dismissDialog() {
        AppAnalytics.create(AnalyticsEvent.ADD_REMINDER_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        cancel()
    }

    fun goToCourse() {
        AppAnalytics.create(AnalyticsEvent.GO_TO_COURSE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        (context as Activity).startActivity(Intent(context, InboxActivity::class.java))
    }

    fun cancel() {
        try {
            val baseDialog = dialog
            if (baseDialog is BottomSheetDialog) {
                val behavior: BottomSheetBehavior<*> = baseDialog.behavior
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        } catch (ex: Exception) {
            dismissAllowingStateLoss()
            ex.printStackTrace()
        }
    }
}

