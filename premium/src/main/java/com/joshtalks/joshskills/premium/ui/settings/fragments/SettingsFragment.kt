package com.joshtalks.joshskills.premium.ui.settings.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.*
import com.joshtalks.joshskills.premium.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.premium.databinding.FragmentSettingsBinding
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.repository.server.LanguageItem
import com.joshtalks.joshskills.premium.ui.extra.AUTO_START_SETTINGS_POPUP
import com.joshtalks.joshskills.premium.ui.settings.SettingsActivity
import com.joshtalks.joshskills.premium.ui.settings.adapter.SettingsAdapter
import com.joshtalks.joshskills.premium.ui.settings.model.Setting
import com.joshtalks.joshskills.premium.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.premium.ui.signup.SignUpActivity
import com.joshtalks.joshskills.voip.data.local.PrefManager as VoipPrefManager

class SettingsFragment : Fragment() {

    private var action: PopupActions? = null
    lateinit var binding: FragmentSettingsBinding

    lateinit var sheetBehaviour: BottomSheetBehavior<*>

    companion object {
        const val TAG = "SettingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)

        binding.lifecycleOwner = this
        binding.handler = this

        sheetBehaviour = BottomSheetBehavior.from(binding.clarDownloadsBottomSheet)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsRecyclerView.adapter = SettingsAdapter(getSettingsList())
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.app_settings))
    }

    fun clearDownloads() {
        MixPanelTracker.publishEvent(MixPanelEvent.CLEAR_ALL_DOWNLOADS).push()
        logEvent(AnalyticsEvent.CLEAR_ALL_DOWNLOADS.name)
        val data =
            workDataOf(MemoryManagementWorker.CLEANUP_TYPE to MemoryManagementWorker.CLEANUP_TYPE_FORCE)
        val workRequest = OneTimeWorkRequestBuilder<MemoryManagementWorker>().addTag("cleanup")
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)

        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun actionConfirmed() {
        when (action) {
            PopupActions.SIGNOUT -> {
                signout()
                if (BuildConfig.DEBUG) {
                    showToast("Signing out")
                }
            }
            PopupActions.LOGIN -> {
                openLoginScreen()
                if (BuildConfig.DEBUG) {
                    showToast("Signing in")
                }
            }
            PopupActions.CLEAR_DOWLOADS -> {
                clearDownloads()
                if (BuildConfig.DEBUG) {
                    showToast("Downloads cleared")
                }
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    showToast("Action didn't match with any condition")
                }
            }
        }
    }

    private fun openLoginScreen() {
        val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
            putExtra(FLOW_FROM, "Settings Screen")
        }
        startActivity(intent)
        requireActivity().finish()
    }

    fun openSelectQualityFragment() {
        MixPanelTracker.publishEvent(MixPanelEvent.DOWNLOAD_QUALITY).push()
        (requireActivity() as BaseActivity).replaceFragment(
            R.id.settings_container, SelectResolutionFragment(), SelectResolutionFragment.TAG,
            TAG
        )
    }

    fun openSelectLanguageFragment() {
        MixPanelTracker.publishEvent(MixPanelEvent.LANGUAGE_CHANGED).push()
        (requireActivity() as BaseActivity).replaceFragment(
            R.id.settings_container, LanguageFragment(), LanguageFragment.TAG, TAG
        )
    }

    fun openPersonalInfoFragment() {
        MixPanelTracker.publishEvent(MixPanelEvent.PERSONAL_INFORMATION).push()
        logEvent(AnalyticsEvent.PERSONAL_PROFILE_CLICKED.name)
        if (User.getInstance().isVerified) {
            (requireActivity() as BaseActivity).replaceFragment(
                R.id.settings_container, PersonalInfoFragment(), PersonalInfoFragment.TAG, TAG
            )
        } else {
            showLoginPopup()
        }
    }

    private fun signout() {
//        showSignoutBottomView()
        (requireActivity() as BaseActivity).logout()
    }

    fun showSignoutBottomView() {
        MixPanelTracker.publishEvent(MixPanelEvent.SIGN_OUT_CLICKED).push()
        binding.clearBtn.text = getString(R.string.sign_out)
        action = PopupActions.SIGNOUT
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_LOGOUT_CONFIRMATION)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun showClearDownloadsView() {
        MixPanelTracker.publishEvent(MixPanelEvent.CLEAR_ALL_DOWNLOADS_CLICKED).push()
        binding.clearBtn.text = getString(R.string.clear_all_downloads)
        action = PopupActions.CLEAR_DOWLOADS
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_CLEAR_DATA_CONFIRMATION)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showLoginPopup() {
        binding.clearBtn.text = getString(R.string.login_signup)
        action = PopupActions.LOGIN
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_SIGN_IN_PROMPT)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun showNotificationSettingPopup() {
        MixPanelTracker.publishEvent(MixPanelEvent.NOTIFICATIONS).push()
        (requireActivity() as SettingsActivity).openAppNotificationSettings()
    }

    fun showAutoStartPermissionPopup() {
        MixPanelTracker.publishEvent(MixPanelEvent.AUTO_START).push()
        (requireActivity() as BaseActivity).checkForOemNotifications(AUTO_START_SETTINGS_POPUP)
    }

    fun hideBottomView() {
        MixPanelTracker.publishEvent(MixPanelEvent.CANCEL).push()
        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun onRateUsClicked() {
        MixPanelTracker.publishEvent(MixPanelEvent.RATE_US).push()
        val uri: Uri =
            Uri.parse("market://details?id=${AppObjectController.joshApplication.packageName}")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${AppObjectController.joshApplication.packageName}")
                    )
                )
            } catch (ex: Exception) {
                showToast("No application found that can handle this link")
            }
        }

        logEvent(AnalyticsEvent.RATE_US_CLICKED.name)
    }

    fun onAboutUsClicked() {
        val url = "https://www.joshtalks.com/about-josh/"
        (activity as BaseActivity).showWebViewDialog(url)

        logEvent(AnalyticsEvent.ABOUT_US.name)
    }

    fun onPrivacyPolicyClicked() {
        MixPanelTracker.publishEvent(MixPanelEvent.PRIVACY_PROFILE).push()
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        (activity as BaseActivity).showWebViewDialog(url)

        logEvent(AnalyticsEvent.PRIVACY_POLICY_CLICKED.name)
    }

    fun onTermsClicked() { //TODO: Update remote config in terms_conditions_url
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        (activity as BaseActivity).showWebViewDialog(url)

        logEvent(AnalyticsEvent.TERMS_CONDITION_CLICKED.name)
    }

    fun onGuidelinesClicked() {
        val url =
            AppObjectController.getFirebaseRemoteConfig().getString("community_guidelines_url")
        (activity as BaseActivity).showWebViewDialog(url)

        logEvent(AnalyticsEvent.COMM_GUIDELINES_CLICKED.name)
    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun getSettingsList(): List<Setting> {
        val list = mutableListOf<Setting>(
            Setting(
                icon = R.drawable.ic_person,
                title = getString(R.string.personal_information),
                onClick = { openPersonalInfoFragment() },
                isDisabled = com.joshtalks.joshskills.premium.core.PrefManager.getBoolValue(
                    com.joshtalks.joshskills.premium.core.IS_FREE_TRIAL,
                    false,
                    false
                ) && User.getInstance().isVerified.not()
            ),
            Setting(
                icon = R.drawable.ic_language,
                title = getString(R.string.language),
                subheading = LanguageItem.getSelectedLanguage(),
                onClick = { openSelectLanguageFragment() }
            ),
            Setting(
                icon = R.drawable.ic_notifications,
                title = getString(R.string.notifications),
                subheading = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.NOTIFICATION_DESCRIPTION_ENABLED),
                onClick = {
                    showNotificationSettingPopup()
                },
                isDisabled = !(requireActivity() as BaseActivity).isNotificationEnabled()
            ),
            Setting(
                icon = R.drawable.ic_autostart,
                title = getString(R.string.autostart),
                subheading = getString(R.string.auto_start_prompt_message),
                onClick = { showAutoStartPermissionPopup() },
                isVisible = PowerManagers.getIntentForOEM(requireContext() as BaseActivity) != null
            ),
            Setting(
                icon = R.drawable.ic_delete,
                title = getString(R.string.clear_all_downloads),
                onClick = { showClearDownloadsView() }
            ),
            Setting(
                icon = R.drawable.ic_download_quality,
                title = getString(R.string.download_quality),
                subheading = com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                    com.joshtalks.joshskills.premium.core.SELECTED_QUALITY
                ),
                onClick = { openSelectQualityFragment() }
            ),
            Setting(
                icon = R.drawable.ic_call_setting,
                title = getString(R.string.p2p_notifications_setting),
                showSwitch = true,
                onSwitch = { isChecked ->
                    com.joshtalks.joshskills.premium.core.PrefManager.put(
                        com.joshtalks.joshskills.premium.core.CALL_RINGTONE_NOT_MUTE, isChecked)
                    MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_PARTNER_NOTIFICATION)
                        .addParam(ParamKeys.IS_CHECKED, isChecked)
                        .push()
                },
                isSwitchChecked = com.joshtalks.joshskills.premium.core.PrefManager.getBoolValue(
                    com.joshtalks.joshskills.premium.core.CALL_RINGTONE_NOT_MUTE
                )
            ),
            Setting(
                icon = R.drawable.proximity,
                title = getString(R.string.p2p_proximity_setting),
                onSwitch = { isChecked ->
                    VoipPrefManager.updateProximitySettings(isChecked)
                },
                showSwitch = true,
                isSwitchChecked = VoipPrefManager.isProximitySensorOn()
            ),
            Setting(
                icon = R.drawable.ic_rate_us,
                title = getString(R.string.rate_us),
                onClick = { onRateUsClicked() }
            ),
            Setting(
                icon = R.drawable.ic_privacy_policy,
                title = getString(R.string.privacy_policy),
                onClick = { onPrivacyPolicyClicked() },
            ),
            Setting(
                icon = R.drawable.ic_terms_condition,
                title = getString(R.string.terms_conditions),
                onClick = { onTermsClicked() }
            ),
            Setting(
                icon = R.drawable.ic_comm_guide,
                title = getString(R.string.community_guidelines),
                onClick = { onGuidelinesClicked() }
            ),
            Setting(
                icon = R.drawable.ic_signout,
                title = getString(R.string.sign_out),
                onClick = { showSignoutBottomView() }
            )
        )
        list.removeIf { !it.isVisible }
        return list.toList()
    }

    enum class PopupActions {
        LOGIN,
        CLEAR_DOWLOADS,
        SIGNOUT
    }
}
