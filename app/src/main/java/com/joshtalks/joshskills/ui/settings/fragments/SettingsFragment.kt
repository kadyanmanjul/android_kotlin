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
import com.joshtalks.joshskills.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.databinding.FragmentSettingsBinding
import com.joshtalks.joshskills.ui.settings.SettingsActivity

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
        binding.notificationSwitch.isChecked = PrefManager.getBoolValue(NOTIFICATION_DISABLED)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.app_settings))
    }

    fun showClearDownloadsView() {
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        binding.clearBtn.text = getString(R.string.clear_all_downloads)
        binding.clearDownloadsBottomTv.text = "All your downloaded media and files will be deleted."
    }

    fun clearDownloads() {
        val data =
            workDataOf(MemoryManagementWorker.CLEANUP_TYPE to MemoryManagementWorker.CLEANUP_TYPE_FORCE)
        val workRequest = OneTimeWorkRequestBuilder<MemoryManagementWorker>().addTag("cleanup")
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)

        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun actionConfirmed() {
        if (binding.clearBtn.text.equals(getString(R.string.sign_out)))
            signout()
        else
            clearDownloads()
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
        showSignoutBottomView()
        (requireActivity() as BaseActivity).logout()
    }

    fun showSignoutBottomView() {
        sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        binding.clearBtn.text = getString(R.string.sign_out)
        binding.clearDownloadsBottomTv.text = "Are you sure you want to signout"
    }

    fun hideBottomView() {
        sheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}