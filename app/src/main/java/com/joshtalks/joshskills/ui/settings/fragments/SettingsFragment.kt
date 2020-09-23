package com.joshtalks.joshskills.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.memory.RemoveMediaWorker
import com.joshtalks.joshskills.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    lateinit var binding: FragmentSettingsBinding

    lateinit var sheetBehaviour: BottomSheetBehavior<*>

    companion object {
        val TAG = "SettingsFragment"
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
            selectedQuality = "360p"
        }

        binding.languageTv.text = selectedLanguage
        binding.downloadQualityTv.text = selectedQuality

        binding.notificationSwitch.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            PrefManager.put(NOTIFICATION_DISABLED, b)
        }
        binding.notificationSwitch.isChecked = PrefManager.getBoolValue(NOTIFICATION_DISABLED)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.app_settings)
    }

    fun showClearDownloadsView() {
        binding.blackShadowIv.visibility = View.VISIBLE
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun clearDownloads() {
        val data = workDataOf("conversation_id" to EMPTY, "time_delete" to false)
        val workRequest = OneTimeWorkRequestBuilder<RemoveMediaWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)

        binding.blackShadowIv.visibility = View.GONE
        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
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

    fun signout() {
        (requireActivity() as BaseActivity).logout()
    }
}