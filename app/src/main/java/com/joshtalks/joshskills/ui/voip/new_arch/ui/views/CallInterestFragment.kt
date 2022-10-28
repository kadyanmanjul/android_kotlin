package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLOSE_INTEREST_ACTIVITY
import com.joshtalks.joshskills.core.INTEREST_FORM_INTEREST_SCREEN_SHOWN
import com.joshtalks.joshskills.core.INTEREST_FORM_SAVED
import com.joshtalks.joshskills.core.INTEREST_FORM_SKIP_PRESSED
import com.joshtalks.joshskills.databinding.FragmentCallInterestBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel

class CallInterestFragment: BaseFragment() {

    lateinit var binding: FragmentCallInterestBinding
    private val interestSet: MutableSet<Int> = hashSetOf()
    private val selectedInterestSet: MutableSet<Int> = hashSetOf()
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
        if (isEditCall){
            binding.skipBtn.visibility = View.GONE
        }else{
            // only send the impression if the form is shown after a call
            viewModel.saveImpression(INTEREST_FORM_INTEREST_SCREEN_SHOWN)
        }
        binding.skipBtn.setOnClickListener {
            viewModel.sendEvent(CLOSE_INTEREST_ACTIVITY)
            viewModel.saveImpression(INTEREST_FORM_SKIP_PRESSED)
        }
        binding.submitBtn.setOnClickListener {
            val id = binding.interestCg.checkedChipIds
            viewModel.sendUserInterest(id)
            viewModel.saveImpression(INTEREST_FORM_SAVED)
            viewModel.sendEvent(CLOSE_INTEREST_ACTIVITY)
        }

        viewModel.interestLiveData.observe(this){
            it.forEach { item->

                val chip = LayoutInflater.from(context).inflate(R.layout.interest_chip_item, null, false) as Chip
                chip.text = item.label
                chip.id = item.id
                chip.typeface = Typeface.create(ResourcesCompat.getFont(requireContext(),R.font.opensans_semibold),Typeface.NORMAL)

                if (item.is_selected == 1){
                    chip.isChecked = true
                    selectedInterestSet.add(chip.id)
                    interestSet.add(chip.id)
                    binding.submitBtn.isEnabled = isEditCall.not()  // if it is not edit call; i.e user had filled from menu, then enable user to submit with pre-selected options
                }


                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked)
                        interestSet.add(buttonView.id)
                    else
                        interestSet.remove(buttonView.id)
                    // two conditions, selected items should be greater than 1 and should not be the same
                    if (isEditCall){
                        binding.submitBtn.isEnabled = (interestSet.size >= minSelection) && interestSet!=selectedInterestSet
                    }else{
                        binding.submitBtn.isEnabled = (interestSet.size >= minSelection)
                    }

                }
                binding.interestCg.addView(chip)
            }
        }
    }

    override fun initViewState() {}

    override fun setArguments() {}

    companion object {
        var isEditCall: Boolean = false

        @JvmStatic
        fun newInstance(isEditCallData: Boolean) =
            CallInterestFragment()
                .apply {
                    arguments = Bundle().apply {
                        isEditCall = isEditCallData
                    }
                }
    }

}