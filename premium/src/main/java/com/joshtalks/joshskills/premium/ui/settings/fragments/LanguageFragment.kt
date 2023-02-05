package com.joshtalks.joshskills.premium.ui.settings.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.EventLiveData
import com.joshtalks.joshskills.premium.constants.DISMISS_PROGRESS_BAR
import com.joshtalks.joshskills.premium.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.premium.core.BaseActivity
import com.joshtalks.joshskills.premium.core.IS_LOCALE_UPDATED_IN_SETTINGS
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.USER_LOCALE
import com.joshtalks.joshskills.premium.databinding.FragmentSelectLanguageBinding
import com.joshtalks.joshskills.premium.repository.server.LanguageItem
import com.joshtalks.joshskills.premium.ui.settings.SettingsActivity
import com.joshtalks.joshskills.premium.ui.settings.adapter.LanguageAdapter

class LanguageFragment : Fragment() {
    lateinit var binding: FragmentSelectLanguageBinding
    private val event = EventLiveData

    companion object {
        const val TAG = "LanguageFragment"
        var progressDialog: ProgressDialog? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_language, container, false)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.languageRv.layoutManager = layoutManager

        val languageList: List<LanguageItem> = LanguageItem.getLanguageList()
        val adapter = LanguageAdapter(
            languageList,
            this::onItemClick
        )
        binding.languageRv.adapter = adapter

        initLiveData()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.select_language))
    }

    private fun initLiveData() {
        event.observe(viewLifecycleOwner) {
            when(it.what) {
                SHOW_PROGRESS_BAR -> showProgressDialog()
                DISMISS_PROGRESS_BAR -> dismissProgressDialog()
            }
        }
    }

    override fun onDestroyView() {
        dismissProgressDialog()
        super.onDestroyView()
    }

    private fun onItemClick(item: LanguageItem) {
        val locale = com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
            com.joshtalks.joshskills.premium.core.USER_LOCALE
        )
        if (locale == item.code) {
            return
        }
        com.joshtalks.joshskills.premium.core.PrefManager.put(com.joshtalks.joshskills.premium.core.IS_LOCALE_UPDATED_IN_SETTINGS,true)
        (requireActivity() as BaseActivity).requestWorkerForChangeLanguage(item.code)
    }

    fun showProgressDialog() {
        progressDialog = ProgressDialog(requireActivity(), R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage("Changing language...")
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()
}