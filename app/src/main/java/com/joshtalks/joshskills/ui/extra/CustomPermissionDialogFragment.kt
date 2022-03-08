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
import com.joshtalks.joshskills.databinding.FragmentCustomPermissionDialogBinding

const val NOTIFICATION_POPUP = "NOTIFICATION_POPUP"
const val AUTO_START_POPUP = "AUTO_START_POPUP"
const val AUTO_START_SETTINGS_POPUP = "AUTO_START_SETTINGS_POPUP"

class CustomPermissionDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCustomPermissionDialogBinding

    companion object {

        lateinit var mIntent: Intent
        var popupType: String = NOTIFICATION_POPUP

        fun newInstance(intent: Intent): CustomPermissionDialogFragment {
            mIntent = intent
            return CustomPermissionDialogFragment()
        }

        fun showCustomPermissionDialog(
            intent: Intent,
            supportFragmentManager: FragmentManager,
            popupType: String
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag("custom_permission_fragment_dialog")
            if (prev != null)
                fragmentTransaction.remove(prev)
            this.popupType = popupType
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
        when(popupType) {
            NOTIFICATION_POPUP -> {
                binding.textView3.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.NOTIFICATION_SETTING_DESCRIPTION)
                binding.popupHeading.visibility = View.GONE
                binding.appCompatImageView.visibility = View.GONE
                logImpression(AnalyticsEvent.NOTIFICATION_SETTINGS_CLICKED)
            }
            AUTO_START_POPUP -> {
                binding.textView3.text = HtmlCompat.fromHtml(getString(R.string.permission_dialog_description), HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (!AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.SHOW_AUTOSTART_IMAGE))
                    binding.appCompatImageView.visibility = View.GONE

                logImpression(AnalyticsEvent.AUTOSTART_CONV_SHOWN)
            }
            AUTO_START_SETTINGS_POPUP -> {
                binding.popupHeading.text = getString(R.string.allow_autostart)
                binding.textView3.text = HtmlCompat.fromHtml(getString(R.string.permission_dialog_description), HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (!AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.SHOW_AUTOSTART_IMAGE))
                    binding.appCompatImageView.visibility = View.GONE

                logImpression(AnalyticsEvent.AUTOSTART_SETTINGS_CLICKED)
            }
        }
    }

    fun allow() {
        dismiss()
        when(popupType) {
            NOTIFICATION_POPUP -> logImpression(AnalyticsEvent.NOTIFICATION_SETTINGS_YES)
            AUTO_START_POPUP -> {
                PrefManager.put(SHOULD_SHOW_AUTOSTART_POPUP, false)
                logImpression(AnalyticsEvent.AUTOSTART_CONV_YES)
            }
            AUTO_START_SETTINGS_POPUP -> logImpression(AnalyticsEvent.AUTOSTART_SETTINGS_YES)
        }
        navigateToSettings()
    }

    fun cancel() {
        dismiss()
        when(popupType) {
            NOTIFICATION_POPUP -> logImpression(AnalyticsEvent.NOTIFICATION_SETTINGS_NO)
            AUTO_START_POPUP -> logImpression(AnalyticsEvent.AUTOSTART_CONV_NO)
            AUTO_START_SETTINGS_POPUP -> logImpression(AnalyticsEvent.AUTOSTART_SETTINGS_NO)
        }
    }

    /** Navigate To Power Manager Settings **/
    fun navigateToSettings() {
        try {
            activity?.startActivity(mIntent)
        } catch (ex: Throwable) {
            dismissAllowingStateLoss()
        }
    }

    fun logImpression(actionPerformed: AnalyticsEvent) {
        (requireActivity() as BaseActivity).pushAnalyticsToServer(actionPerformed.NAME)
    }
}
