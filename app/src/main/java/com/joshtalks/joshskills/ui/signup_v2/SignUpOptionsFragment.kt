package com.joshtalks.joshskills.ui.signup_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.databinding.FragmentSignUpOptionsBinding
import com.joshtalks.joshskills.ui.signup.DEFAULT_COUNTRY_CODE
import io.reactivex.disposables.CompositeDisposable


private const val MOBILE_NUMBER_HINT_REQUEST_CODE = 9001

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

    private fun mobileNumberHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .setEmailAddressIdentifierSupported(false)
            .build()
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        val pendingIntent =
            Credentials.getClient(requireContext(), options).getHintPickerIntent(hintRequest)
        startIntentSenderForResult(
            pendingIntent.intentSender,
            MOBILE_NUMBER_HINT_REQUEST_CODE,
            null,
            0,
            0,
            0,
            null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val credential: Credential? =
                data?.getParcelableExtra(Credential.EXTRA_KEY)
            credential?.id?.run {
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

}
