package com.joshtalks.joshskills.ui.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CUSTOM_PERMISSION_ACTION_KEY
import com.joshtalks.joshskills.core.PermissionAction
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentCustomPermissionDialogBinding

class CustomPermissionDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCustomPermissionDialogBinding
    private val interactionListener by lazy { activity as CustomPermissionDialogInteractionListener }

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
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_custom_permission_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
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
        interactionListener.navigateToNextScreen()
    }

    fun doNotAskAgain() {
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.DO_NOT_ASK_AGAIN.name)
        logAction(PermissionAction.DO_NOT_ASK_AGAIN)
        dismiss()
        interactionListener.navigateToNextScreen()
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
