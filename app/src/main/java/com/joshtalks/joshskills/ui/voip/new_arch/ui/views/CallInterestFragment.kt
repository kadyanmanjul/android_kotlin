package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLOSE_INTEREST_ACTIVITY
import com.joshtalks.joshskills.databinding.FragmentCallInterestBinding
import com.joshtalks.joshskills.repository.server.onboarding.CourseInterestTag
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel

class CallInterestFragment: BaseFragment() {

    lateinit var binding: FragmentCallInterestBinding
    private val interestSet: MutableSet<Int> = hashSetOf()
    private var minSelection = 1
    private val viewModel by lazy { ViewModelProvider(this)[CallInterestViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_call_interest, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.skipBtn.setOnClickListener {
            viewModel.sendEvent(CLOSE_INTEREST_ACTIVITY)
        }

        binding.submitBtn.setOnClickListener {
            val id = binding.interestCg.checkedChipIds
            viewModel.sendUserInterest(id)
            viewModel.sendEvent(CLOSE_INTEREST_ACTIVITY)
        }

        viewModel.interestLiveData.observe(this){
            it.forEach { item->
                val chip = LayoutInflater.from(context).inflate(R.layout.interest_chip_item, null, false) as Chip
                chip.text = item.label
                chip.id = item.id
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

    override fun initViewState() {
    }

    override fun setArguments() {
    }
}