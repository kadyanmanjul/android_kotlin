package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentCallInterestBinding
import com.joshtalks.joshskills.repository.server.onboarding.CourseInterestTag

class CallInterestFragment: BaseFragment() {

    lateinit var binding: FragmentCallInterestBinding
    private val interestSet: MutableSet<Int> = hashSetOf()
    private var minSelection = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_call_interest, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        populateInterests()
    }

    override fun initViewState() {
        //TODO("Not yet implemented")
    }

    override fun setArguments() {
        //TODO("Not yet implemented")
    }

    private fun populateInterests() {
        //TODO: Fetch tags either from backend or remote config
        val courseInterestTags: List<CourseInterestTag> = listOf()
        courseInterestTags.forEach {
            val chip = LayoutInflater.from(context).inflate(R.layout.interest_chip_item, null, false) as Chip
            chip.text = it.name
            chip.tag = it.id
            chip.id = it.id!!
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked)
                    interestSet.add(buttonView.id)
                else
                    interestSet.remove(buttonView.id)
                binding.submitBtn.isEnabled = interestSet.size >= minSelection

            }
            binding.interestCg.addView(chip)
        }

    }
}