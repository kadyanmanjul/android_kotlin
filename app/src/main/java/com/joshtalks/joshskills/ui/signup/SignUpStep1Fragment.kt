package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.*
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.SignUpStep1FragmentBinding


const val RC_HINT = 2
const val DEFAULT_COUNTRY_CODE = "IN"

val PHONE_NUMBER_REGEX = Regex(pattern = "^[6789]\\d{9}\$")

class SignUpStep1Fragment : Fragment() {

    companion object {
        fun newInstance() = SignUpStep1Fragment()
    }

    private lateinit var signUpStep1FragmentBinding: SignUpStep1FragmentBinding
    private lateinit var viewModel: SignUpViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run { ViewModelProvider(this).get(SignUpViewModel::class.java) }
            ?: throw Exception("Invalid Activity")
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        signUpStep1FragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.sign_up_step1_fragment, container, false)
        signUpStep1FragmentBinding.lifecycleOwner = this
        signUpStep1FragmentBinding.handler = this
        return signUpStep1FragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<View>(R.id.back_tv)?.visibility = View.GONE
        signUpStep1FragmentBinding.countryCodePicker.setDefaultCountryUsingNameCode(
            DEFAULT_COUNTRY_CODE
        )
        signUpStep1FragmentBinding.countryCodePicker.setAutoDetectedCountry(true)
        signUpStep1FragmentBinding.countryCodePicker.setCountryForNameCode(DEFAULT_COUNTRY_CODE)
        signUpStep1FragmentBinding.countryCodePicker.setDetectCountryWithAreaCode(true)
        signUpStep1FragmentBinding.mobileEt.prefix =
            signUpStep1FragmentBinding.countryCodePicker.defaultCountryCodeWithPlus

        signUpStep1FragmentBinding.countryCodePicker.setOnCountryChangeListener {
            signUpStep1FragmentBinding.mobileEt.prefix =
                signUpStep1FragmentBinding.countryCodePicker.selectedCountryCodeWithPlus
            AppAnalytics.create(AnalyticsEvent.COUNTRY_FLAG_CHANGED.NAME)
                .addParam("MobilePrefix",signUpStep1FragmentBinding.mobileEt.prefix)
                .push()
        }

        initTermsConditionView()
        initProgressView()
        requestHint()
        signUpStep1FragmentBinding.mobileEt.setText(viewModel.phoneNumber)
        viewModel.progressDialogStatus.observe(viewLifecycleOwner, Observer {
            if (it.not()) {
                hideProgress()
            }
        })
    }


    private fun initTermsConditionView() {
        val spannableString = SpannableString(getString(R.string.terms_and_condition_msz))
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        spannableString.setSpan(
            URLSpan(url),
            74,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        signUpStep1FragmentBinding.termsconditiontv.text = spannableString
        signUpStep1FragmentBinding.termsconditiontv.movementMethod =
            LinkMovementMethod.getInstance()
        // TODO Add Terms and Condition Analytics  EVENt -TERMS_CONDITION_CLICKED
    }

    private fun initProgressView() {
        bindProgressButton(signUpStep1FragmentBinding.btnLogin)
        signUpStep1FragmentBinding.btnLogin.attachTextChangeAnimator()

    }

    fun sendOtpToMobile() {
        if (signUpStep1FragmentBinding.mobileEt.text.isNullOrEmpty()) {
            Toast.makeText(
                AppObjectController.joshApplication,
                getString(R.string.please_enter_your_mobile_number),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        if (validationForIndiaOnly() && validPhoneNumber(signUpStep1FragmentBinding.mobileEt.text.toString()).not()) {
            signUpStep1FragmentBinding.inputLayoutPassword.error = "Please enter valid phone number"
            signUpStep1FragmentBinding.inputLayoutPassword.isErrorEnabled = true
            return
        }
        signUpStep1FragmentBinding.inputLayoutPassword.isErrorEnabled = false
        showProgress()
        viewModel.networkCallForOtp(
            signUpStep1FragmentBinding.mobileEt.prefix,
            signUpStep1FragmentBinding.mobileEt.text.toString()
        )
        AppAnalytics.create(AnalyticsEvent.NEXT_TO_OTP_SCREEN_CLICKED.NAME)
            .addParam("MobilePrefix",signUpStep1FragmentBinding.mobileEt.prefix)
            .addParam("MobileNmberEntered",signUpStep1FragmentBinding.mobileEt.text.toString())
            .push()

    }

    private fun validPhoneNumber(number: String): Boolean {
        return PHONE_NUMBER_REGEX.containsMatchIn(input = number)
    }

    private fun validationForIndiaOnly(): Boolean {
        return signUpStep1FragmentBinding.mobileEt.prefix.trim() == "+91"
    }


    private fun showProgress() {
        signUpStep1FragmentBinding.btnLogin.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(Color.WHITE, Color.RED, Color.GREEN)
            gravity = DrawableButton.GRAVITY_TEXT_END
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        signUpStep1FragmentBinding.btnLogin.isEnabled = false

    }

    private fun hideProgress() {
        try {
            signUpStep1FragmentBinding.btnLogin.isEnabled = true
            signUpStep1FragmentBinding.btnLogin.hideProgress(getString(R.string.next))
        } catch (e: Exception) {
        }
    }

    private fun requestHint() {

        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .setEmailAddressIdentifierSupported(false)

            .build()
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        val pendingIntent = Credentials.getClient(AppObjectController.joshApplication, options)
            .getHintPickerIntent(hintRequest)
        startIntentSenderForResult(pendingIntent.intentSender, RC_HINT, null, 0, 0, 0, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            val credential: Credential? =
                data?.getParcelableExtra(Credential.EXTRA_KEY)

            signUpStep1FragmentBinding.mobileEt.setText(
                credential?.id?.replaceFirst(
                    signUpStep1FragmentBinding.mobileEt.prefix,
                    EMPTY
                )
            )
        } catch (ex: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            signUpStep1FragmentBinding.mobileEt.setSelection(signUpStep1FragmentBinding.mobileEt.text!!.length)
        } catch (ex: Exception) {
        }
    }


}
