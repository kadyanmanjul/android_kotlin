package com.joshtalks.joshskills.ui.signup_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentSignUpOptionsBinding
import com.joshtalks.joshskills.ui.signup.DEFAULT_COUNTRY_CODE
import io.reactivex.disposables.CompositeDisposable

class SignUpOptionsFragment : Fragment() {

    private var compositeDisposable = CompositeDisposable()
    private lateinit var viewModel: SignUpV2ViewModel
    private lateinit var binding: FragmentSignUpOptionsBinding

    companion object {
        fun newInstance() = SignUpOptionsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SignUpV2ViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_options, container, false)
        binding.lifecycleOwner = this
        binding.handler = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.countryCodePicker.setDefaultCountryUsingNameCode(
            DEFAULT_COUNTRY_CODE
        )
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setCountryForNameCode(DEFAULT_COUNTRY_CODE)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        binding.mobileEt.prefix = binding.countryCodePicker.defaultCountryCodeWithPlus
        binding.countryCodePicker.setOnCountryChangeListener {
            binding.mobileEt.prefix = binding.countryCodePicker.selectedCountryCodeWithPlus
        }
    }

    fun loginViaGoogle() {

    }

    fun loginViaFacebook() {

    }

    fun loginViaPhoneNumber() {

    }
}
