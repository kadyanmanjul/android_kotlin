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
import com.joshtalks.joshskills.databinding.FragmentSelectLanguageBinding
import com.joshtalks.joshskills.ui.settings.adapter.ACTION_QUALITY
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
                ACTION_QUALITY,
                resources.getStringArray(R.array.resolutions),
                this::onItemClick
            )
        binding.languageRv.adapter = adapter
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.download_quality)
    }

    fun onItemClick(item: String, position: Int): Unit {
        PrefManager.put(SELECTED_QUALITY, item)
    }

}