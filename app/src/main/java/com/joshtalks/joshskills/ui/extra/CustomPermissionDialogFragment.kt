package com.joshtalks.joshskills.ui.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
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

        /**
         *  Show fragment asking for custom permission to start app in background for proper working of notifications
         */
        fun showCustomPermissionDialog(intent: Intent, supportFragmentManager: FragmentManager) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag("custom_permission_fragment_dialog")
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(intent)
                .show(supportFragmentManager, "custom_permission_fragment_dialog")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_custom_permission_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        binding.textView3.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.NOTIFICATION_SETTING_DESCRIPTION)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.DO_NOT_ASK_AGAIN.name)
    }

    fun allow() {
        PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.ALLOW.name)
        logAction(PermissionAction.ALLOW)
        dismiss()
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
        try {
            activity?.startActivity(mIntent)
        } catch (ex: Throwable) {
            PrefManager.put(CUSTOM_PERMISSION_ACTION_KEY, PermissionAction.DO_NOT_ASK_AGAIN.name)
            dismissAllowingStateLoss()
        }
    }

    fun logAction(actionPerformed: PermissionAction) {
        AppAnalytics.create(AnalyticsEvent.CUSTOM_PERMISSION_ACTION.NAME)
            .addParam("Action", actionPerformed.name)
            .push()
    }
}
