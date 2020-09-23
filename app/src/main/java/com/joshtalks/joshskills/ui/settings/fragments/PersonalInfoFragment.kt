package com.joshtalks.joshskills.ui.settings.fragments

import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.settings.SettingsActivity

class PersonalInfoFragment : Fragment() {


    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.personal_information))
    }
}