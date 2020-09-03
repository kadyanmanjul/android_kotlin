package com.joshtalks.joshskills.ui.new_onboarding.onboarding2

import android.os.Bundle
import androidx.fragment.app.Fragment

class SelectInterestFragment : Fragment() {

    companion object {
        fun newInstance(
            maxInterest: Int,
            minInterest: Int,
            interestList: List<String>
        ): SelectInterestFragment {
            val args = Bundle()

            val fragment = SelectInterestFragment()
            fragment.arguments = args
            return fragment
        }
    }
}