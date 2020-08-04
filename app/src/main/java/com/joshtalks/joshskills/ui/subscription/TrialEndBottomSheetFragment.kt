package com.joshtalks.joshskills.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentTrialEndBottomsheetBinding
import com.joshtalks.joshskills.repository.local.model.ExploreCardType

class TrialEndBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTrialEndBottomsheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_trial_end_bottomsheet,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    fun cancel() {
        try {
            requireActivity().finish()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun unlockCourses() {
        StartSubscriptionActivity.startActivity(requireActivity(), 0, ExploreCardType.SUBSCRIPTION)
    }

    companion object {
        @JvmStatic
        fun newInstance() = TrialEndBottomSheetFragment()
    }

}
