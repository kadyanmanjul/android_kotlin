package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentSelectInterestBinding
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import kotlinx.android.synthetic.main.base_toolbar.view.text_message_title

class SelectInterestFragment : Fragment() {

    lateinit var binding: FragmentSelectInterestBinding
    private val interestSet: MutableSet<Int> = hashSetOf()
    lateinit var viewmodel: OnBoardViewModel
    private var maxSelection = 5
    private var minSelection = 3

    companion object {
        const val TAG = "SelectInterestFragment"
        fun newInstance(
        ): SelectInterestFragment {
            val args = Bundle()

            val fragment = SelectInterestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewmodel = ViewModelProvider(requireActivity()).get(OnBoardViewModel::class.java)
        maxSelection =
            (requireActivity() as BaseActivity).getVersionData()?.maximumNumberOfInterests!!
        minSelection =
            (requireActivity() as BaseActivity).getVersionData()?.minimumNumberOfInterests!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_interest, container, false)
        binding.handler = this
        binding.interestDescriptionTv.text =
            (requireActivity() as BaseActivity).getVersionData()?.interestText
        populateInterests()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.toolbar.text_message_title.text = getString(R.string.select_interest)
    }

    fun onDoneClicked() {
        viewmodel.apiCallStatusLiveData.observe(viewLifecycleOwner, Observer {
            if (it == ApiCallStatus.SUCCESS) {
                showBottomDialog()
            }
        })
        viewmodel.enrollMentorAgainstTags(interestSet.toList())
    }

    private fun showBottomDialog() {
        (requireActivity() as BaseActivity).apply {
            replaceFragment(
                R.id.onboarding_container,
                SuccessfulEnrolledBottomSheet.newInstance(),
                SuccessfulEnrolledBottomSheet.TAG
            )
        }
    }


    private fun populateInterests() {
        (requireActivity() as BaseActivity).getVersionData()?.courseInterestTags?.forEach {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.interest_chip_item, null, false) as Chip
            chip.text = it.name
            chip.tag = it.id
            chip.id = it.id!!
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (interestSet.size == maxSelection && isChecked) {
                    buttonView.isChecked = false
                    showToast("Max selection reached.")
                } else {

                    if (isChecked)
                        interestSet.add(buttonView.id)
                    else
                        interestSet.remove(buttonView.id)
                    binding.interestBtn.isEnabled = interestSet.size >= minSelection

                    binding.selectedInterestTv.text =
                        getString(R.string.interest_count, interestSet.size, maxSelection)
                }
            }
            binding.interestCg.addView(chip)
        }

    }
}
