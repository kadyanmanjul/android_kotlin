package com.joshtalks.joshskills.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SELECTED_QUALITY
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.FragmentSelectLanguageBinding
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.settings.adapter.StringAdapter

class SelectResolutionFragment : Fragment() {
    companion object {
        val TAG = "SelectResolutionFragment"
    }

    lateinit var binding: FragmentSelectLanguageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_language, container, false)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.languageRv.layoutManager = layoutManager
        val adapter =
            StringAdapter(
                resources.getStringArray(R.array.resolutions),
                this::onItemClick
            )
        binding.languageRv.adapter = adapter
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.download_quality))
    }

    fun onItemClick(item: String, position: Int): Unit {
        when(item) {
            "High" -> MixPanelTracker.publishEvent(MixPanelEvent.HIGH).push()
            "Medium" -> MixPanelTracker.publishEvent(MixPanelEvent.MEDIUM).push()
            "Low" -> MixPanelTracker.publishEvent(MixPanelEvent.LOW).push()
        }
        PrefManager.put(SELECTED_QUALITY, item)
    }

}