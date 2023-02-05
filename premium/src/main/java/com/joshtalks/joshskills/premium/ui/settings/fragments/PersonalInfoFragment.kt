package com.joshtalks.joshskills.premium.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.GENDER
import com.joshtalks.joshskills.premium.core.RegistrationMethods
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.databinding.FragmentPersonalInfoBinding
import com.joshtalks.joshskills.premium.repository.local.eventbus.CreatedSource
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.settings.SettingsActivity

class PersonalInfoFragment : Fragment() {
    companion object {
        val TAG = "PersonalInfoFragment"
    }

    lateinit var binding: FragmentPersonalInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_personal_info, container, false)

        populateData()
        return binding.root
    }

    private fun populateData() {
        val user = Mentor.getInstance().getUser()
        binding.nameTv.text = user?.firstName

        user?.dateOfBirth?.let {
            binding.dobTv.text = Utils.formatDate(it, "yyyy-MM-dd", "dd/MM/yyyy")
        }
        when (user?.gender ?: "") {
            GENDER.MALE.gValue ->
                binding.genderTv.text = getString(R.string.male)
            GENDER.FEMALE.gValue ->
                binding.genderTv.text = getString(R.string.female)
            GENDER.OTHER.gValue ->
                binding.genderTv.text = getString(R.string.other)
        }
        when (user?.source) {
            CreatedSource.FB.name -> {
                binding.loginTypeTv.text = RegistrationMethods.FACEBOOK.type
            }
            CreatedSource.GML.name -> {
                binding.loginTypeTv.text = RegistrationMethods.GOOGLE.type
            }
            CreatedSource.OTP.name -> {
                binding.loginTypeTv.text = RegistrationMethods.MOBILE_NUMBER.type
            }
            CreatedSource.TC.name -> {
                binding.loginTypeTv.text = RegistrationMethods.TRUE_CALLER.type
            }
        }
        if (Mentor.getInstance().getUser()?.phoneNumber.isNullOrBlank().not()) {
            binding.registerTv.text = Mentor.getInstance().getUser()?.phoneNumber
            binding.textView6.text = getString(R.string.register_number)
        } else if (Mentor.getInstance().getUser()?.email.isNullOrBlank().not()) {
            binding.registerTv.text = Mentor.getInstance().getUser()?.email
            binding.textView6.text = getString(R.string.register_email)
        } else {
            binding.registerTv.visibility = View.GONE
            binding.textView6.visibility = View.GONE
        }

        binding.appVersionTv.text = BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as SettingsActivity).setTitle(getString(R.string.personal_information))
    }
}