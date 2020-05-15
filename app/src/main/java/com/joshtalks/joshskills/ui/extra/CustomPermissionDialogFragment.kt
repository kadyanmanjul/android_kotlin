package com.joshtalks.joshskills.ui.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CUSTOM_PERMISSION_ACTION_KEY
import com.joshtalks.joshskills.core.PermissionAction
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics

class CustomPermissionDialogFragment : BottomSheetDialogFragment() {

    companion object {
        lateinit var mIntent: Intent
        fun newInstance(intent: Intent): CustomPermissionDialogFragment {
            mIntent = intent
            return CustomPermissionDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_permission_dialog, container, false)
    }

    fun allow() {
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.ALLOW.name)
        logAction(PermissionAction.ALLOW)
        navigateToSettings()
    }

    fun cancel() {
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.CANCEL.name)
        logAction(PermissionAction.CANCEL)
        dismiss()
    }

    fun doNotAskAgain() {
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.DO_NOT_ASK_AGAIN.name)
        logAction(PermissionAction.DO_NOT_ASK_AGAIN)
        dismiss()
    }

    /**
     * Navigate To Power Manager Settings
     * */
    fun navigateToSettings() {
        activity?.startActivity(mIntent)

    }

    fun logAction(actionPerformed: PermissionAction) {
        AppAnalytics.create(AnalyticsEvent.CUSTOM_PERMISSION_ACTION.NAME)
            .addParam("Action", actionPerformed.name)
            .push()
    }
}
