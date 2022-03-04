package com.joshtalks.joshskills.ui.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentCustomPermissionDialogBinding

const val OPEN_NOTIFICATION = "OPEN_NOTIFICATION"
const val OPEN_AUTO_START = "OPEN_AUTO_START"
const val OPEN_AUTO_START_SETTINGS = "OPEN_AUTO_START_SETTINGS"

class CustomPermissionDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCustomPermissionDialogBinding

    companion object {

        lateinit var mIntent: Intent
        var eventType: String = OPEN_NOTIFICATION

        fun newInstance(intent: Intent): CustomPermissionDialogFragment {
            mIntent = intent
            return CustomPermissionDialogFragment()
        }

        fun showCustomPermissionDialog(
            intent: Intent,
            supportFragmentManager: FragmentManager,
            eventType: String
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag("custom_permission_fragment_dialog")
            if (prev != null)
                fragmentTransaction.remove(prev)
            this.eventType = eventType
            fragmentTransaction.addToBackStack(null)
            newInstance(intent).show(supportFragmentManager, "custom_permission_fragment_dialog")
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

        initPopupUI()
        return binding.root
    }

    private fun initPopupUI() {
        when(eventType) {
            OPEN_NOTIFICATION -> {
                binding.textView3.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.NOTIFICATION_SETTING_DESCRIPTION)
                binding.popupHeading.visibility = View.GONE
                binding.appCompatImageView.visibility = View.GONE
            }
            OPEN_AUTO_START -> {
                binding.textView3.text = HtmlCompat.fromHtml(getString(R.string.permission_dialog_description), HtmlCompat.FROM_HTML_MODE_LEGACY)

                if (!AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.SHOW_AUTOSTART_IMAGE))
                    binding.appCompatImageView.visibility = View.GONE
            }
            OPEN_AUTO_START_SETTINGS -> {
                binding.popupHeading.text = getString(R.string.allow_autostart)
                binding.textView3.text = HtmlCompat.fromHtml(getString(R.string.permission_dialog_description), HtmlCompat.FROM_HTML_MODE_LEGACY)

                if (!AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.SHOW_AUTOSTART_IMAGE))
                    binding.appCompatImageView.visibility = View.GONE
            }
        }
    }

    fun allow() {
        PrefManager.put(SHOULD_SHOW_AUTOSTART_POPUP, false)
        logAction(PermissionAction.ALLOW)
        dismiss()
        navigateToSettings()
    }

    fun cancel() {
        logAction(PermissionAction.CANCEL)
        dismiss()
    }

    fun doNotAskAgain() {
        logAction(PermissionAction.DO_NOT_ASK_AGAIN)
        dismiss()
    }

    /** Navigate To Power Manager Settings **/
    fun navigateToSettings() {
        try {
            activity?.startActivity(mIntent)
        } catch (ex: Throwable) {
            dismissAllowingStateLoss()
        }
    }

    fun logAction(actionPerformed: PermissionAction) {
        AppAnalytics.create(AnalyticsEvent.CUSTOM_PERMISSION_ACTION.NAME)
            .addParam("Action", actionPerformed.name)
            .push()
    }
}
