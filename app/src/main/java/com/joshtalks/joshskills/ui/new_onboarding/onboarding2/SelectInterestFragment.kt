package com.joshtalks.joshskills.ui.new_onboarding.onboarding2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentSelectInterestBinding

class SelectInterestFragment : Fragment() {

    lateinit var binding: FragmentSelectInterestBinding

    private val interestSet: MutableSet<Int> = hashSetOf()

    companion object {
        const val TAG = "SelectInterestFragment"
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

        binding.interestCg.setOnCheckedChangeListener { _, checkedId ->
            if (interestSet.contains(checkedId))
                interestSet.remove(checkedId)
            else
                interestSet.add(checkedId)
            binding.selectedInterestTv.text =
                getString(R.string.interest_count, interestSet.size, 5)
        }
        populateInterests()
        return binding.root
    }

    private fun populateInterests() {
        for (i in 1..10) {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.interest_chip_item, null, false) as Chip
            chip.text = "Category Name"
            chip.tag = i
            chip.id = i
            binding.interestCg.addView(chip)
        }
    }
}
