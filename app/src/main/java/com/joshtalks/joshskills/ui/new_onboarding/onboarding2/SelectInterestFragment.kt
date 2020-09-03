package com.joshtalks.joshskills.ui.new_onboarding.onboarding2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentSelectInterestBinding

class SelectInterestFragment : Fragment() {

    lateinit var binding: FragmentSelectInterestBinding

    companion object {
        fun newInstance(
            maxInterest: Int,
            minInterest: Int
        ): SelectInterestFragment {
            val args = Bundle()

            val fragment = SelectInterestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_interest, container, false)

        binding.interestCg.setOnCheckedChangeListener(ChipGroup.OnCheckedChangeListener { group, checkedId -> })
        populateInterests()
        return binding.root
    }

    private fun populateInterests() {
        for (i in 1..10) {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.faq_category_item, null, false) as Chip
            chip.text = "Category Name"
            chip.tag = i
            chip.id = i
            binding.interestCg.addView(chip)
        }
    }
}
