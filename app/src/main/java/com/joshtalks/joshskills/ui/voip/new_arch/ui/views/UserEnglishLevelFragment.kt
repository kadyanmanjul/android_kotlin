package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.LAYOUT_DIRECTION_LOCALE
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.START_USER_INTEREST_FRAGMENT
import com.joshtalks.joshskills.core.INTEREST_FORM_LEVEL_SAVED
import com.joshtalks.joshskills.core.INTEREST_FORM_LEVEL_SCREEN_OPEN
import com.joshtalks.joshskills.databinding.FragmentUserEnglishLevelBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel

class UserEnglishLevelFragment : BaseFragment() {

    lateinit var binding : FragmentUserEnglishLevelBinding
    private val viewModel by lazy { ViewModelProvider(this)[CallInterestViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_user_english_level, container, false)
        return binding.root
    }
    override fun initViewBinding() {
        //viewModel.getUserLevelDetails()
        viewModel.saveImpression(INTEREST_FORM_LEVEL_SCREEN_OPEN)
        binding.btnContinue.setOnClickListener {
            val level = binding.levelChipGroup.checkedChipId
            viewModel.sendUserLevel(level)
            viewModel.sendEvent(START_USER_INTEREST_FRAGMENT)
            viewModel.saveImpression(INTEREST_FORM_LEVEL_SAVED)
        }

        binding.levelChipGroup.layoutDirection

        viewModel.levelLiveData.observe(this){
            binding.levelChipGroup.removeAllViews() // to ensure there are no duplicates incase fragment is called from backstack

            it.forEach { hash->
                val chip = LayoutInflater.from(context).inflate(R.layout.english_level_chip_item, null, false) as Chip
                chip.text = hash["label"]
                chip.id = hash["id"]?.toInt()?:0

                chip.setOnCheckedChangeListener { _, isChecked ->
                    binding.btnContinue.isEnabled = isChecked
                }
                binding.levelChipGroup.addView(chip)
            }
        }

    }

    override fun initViewState() {}

    override fun setArguments() {}
}