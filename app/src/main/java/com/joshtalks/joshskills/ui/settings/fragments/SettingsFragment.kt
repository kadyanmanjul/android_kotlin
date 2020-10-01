package com.joshtalks.joshskills.ui.settings.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.databinding.FragmentSettingsBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity

class SettingsFragment : Fragment() {

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

        var selectedLanguage = PrefManager.getStringValue(SELECTED_LANGUAGE)
        var selectedQuality = PrefManager.getStringValue(SELECTED_QUALITY)

        if (selectedLanguage.isEmpty()) {
            selectedLanguage = "English"
        }
        if (selectedQuality.isEmpty()) {
            selectedQuality = resources.getStringArray(R.array.resolutions).get(2) ?: "Low"
        }

        binding.languageTv.text = selectedLanguage
        binding.downloadQualityTv.text = selectedQuality

        if (User.getInstance().isVerified.not()) {
            binding.signOutTv.visibility = View.GONE
        }

        sheetBehaviour.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    binding.blackShadowIv.visibility = View.GONE
                else
                    binding.blackShadowIv.visibility = View.VISIBLE
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        }
        )

        if ((requireActivity() as BaseActivity).shouldRequireCustomPermission()) {
            binding.notificationStatusTv.setText(R.string.off)
            binding.notificationDiscription.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.NOTIFICATION_DESCRIPTION_DISABLED)
            binding.notificationStatusTv.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.rounded_grey_bg_2dp)
        } else {
            binding.notificationRightIv.visibility = View.GONE
            binding.notificationStatusTv.setText(R.string.on)
            binding.notificationDiscription.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.NOTIFICATION_DESCRIPTION_ENABLED)
            binding.notificationStatusTv.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.rounded_primary_bg_2dp)
        }


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.app_settings))
    }

    fun clearDownloads() {
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
        when (binding.clearBtn.text) {
            getString(R.string.sign_out) -> {
                signout()
            }
            getString(R.string.login_signup) -> {
                openLoginScreen()
            }
            else -> clearDownloads()
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
        (requireActivity() as BaseActivity).replaceFragment(
            R.id.settings_container, SelectResolutionFragment(), SelectResolutionFragment.TAG,
            TAG
        )
    }

    fun openSelectLanguageFragment() {
        (requireActivity() as BaseActivity).replaceFragment(
            R.id.settings_container, LanguageFragment(), LanguageFragment.TAG,
            TAG
        )
    }

    fun openPersonalInfoFragment() {
        logEvent(AnalyticsEvent.PERSONAL_PROFILE_CLICKED.name)
        if (User.getInstance().isVerified) {
            (requireActivity() as BaseActivity).replaceFragment(
                R.id.settings_container, PersonalInfoFragment(), PersonalInfoFragment.TAG,
                TAG
            )
        } else {
            showLoginPopup()
        }
    }

    private fun signout() {
        showSignoutBottomView()
        (requireActivity() as BaseActivity).logout()
    }

    fun showSignoutBottomView() {
        binding.clearBtn.text = getString(R.string.sign_out)
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_LOGOUT_CONFIRMATION)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun showClearDownloadsView() {
        binding.clearBtn.text = getString(R.string.clear_all_downloads)
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_CLEAR_DATA_CONFIRMATION)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showLoginPopup() {
        binding.clearBtn.text = getString(R.string.login_signup)
        binding.clearDownloadsBottomTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SETTINGS_SIGN_IN_PROMPT)
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun showNotificationSettingPopup() {
        (requireActivity() as BaseActivity).checkForOemNotifications()
    }

    fun hideBottomView() {
        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }
}