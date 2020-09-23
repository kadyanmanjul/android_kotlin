package com.joshtalks.joshskills.ui.settings.fragments

import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R

class PersonalInfoFragment : Fragment() {


    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.personal_information)
    }
}