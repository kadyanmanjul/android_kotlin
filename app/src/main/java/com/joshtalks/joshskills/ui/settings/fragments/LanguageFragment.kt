package com.joshtalks.joshskills.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SELECTED_LANGUAGE
import com.joshtalks.joshskills.databinding.FragmentSelectLanguageBinding
import com.joshtalks.joshskills.repository.server.LanguageItem
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.settings.adapter.LanguageAdapter
import com.sinch.gson.reflect.TypeToken
import java.lang.reflect.Type

class LanguageFragment : Fragment() {
    lateinit var binding: FragmentSelectLanguageBinding

    companion object {
        val TAG = "LanguageFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_language, container, false)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.languageRv.layoutManager = layoutManager

        val listType: Type = object : TypeToken<List<LanguageItem>>() {}.type
        val languageList: List<LanguageItem> = Gson().fromJson(
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.LANGUAGES_SUPPORTED
            ), listType
        )
        val adapter = LanguageAdapter(
            languageList,
            this::onItemClick
        )
        binding.languageRv.adapter = adapter
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.select_language))
    }

    private fun onItemClick(item: LanguageItem, position: Int) {
        PrefManager.put(SELECTED_LANGUAGE, item.code)
    }

}