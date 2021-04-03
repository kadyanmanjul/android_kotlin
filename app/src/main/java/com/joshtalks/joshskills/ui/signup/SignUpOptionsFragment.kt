package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.TIMEOUT_TIME
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.VerificationService
import com.joshtalks.joshskills.core.VerificationStatus
import com.joshtalks.joshskills.core.VerificationVia
import com.joshtalks.joshskills.core.getCountryIsoCode
import com.joshtalks.joshskills.core.hideKeyboard
import com.joshtalks.joshskills.core.isValidFullNumber
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentSignUpOptionsBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.sinch.verification.PhoneNumberUtils
import java.util.concurrent.TimeUnit

class SignUpOptionsFragment : BaseSignUpFragment() {

    private var prefix: String = EMPTY
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpOptionsBinding
    private var timer: CountDownTimer? = null
    private var verificationVia: VerificationVia = VerificationVia.SMS
    private var verificationService: VerificationService = VerificationService.SMS_COUNTRY
    private var lastTime = 0

    companion object {
        fun newInstance() = SignUpOptionsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
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
        val defaultRegion: String = PhoneNumberUtils.getDefaultCountryIso(requireContext())
        setupVerificationSystem(defaultRegion)
        if (Utils.isTrueCallerAppExist()) {
            binding.btnTruecallerLogin.visibility = View.VISIBLE
        }
        binding.countryCodePicker.setAutoDetectedCountry(false)
        binding.countryCodePicker.setDetectCountryWithAreaCode(false)
        binding.countryCodePicker.setOnCountryChangeListener {
            prefix = binding.countryCodePicker.selectedCountryCodeWithPlus
            setupVerificationSystem(binding.countryCodePicker.selectedCountryNameCode)
        }
        val supportedCountryList =
            AppObjectController.getFirebaseRemoteConfig().getString("SUPPORTED_COUNTRY_LIST")
        if (supportedCountryList.isNotEmpty()) {
            binding.countryCodePicker.setCustomMasterCountries(supportedCountryList)
        }
        prefix = binding.countryCodePicker.getCountryCodeByName(defaultRegion)
        bindProgressButton(binding.btnLogin)
        viewModel.verificationStatus.observe(viewLifecycleOwner, Observer {
            it.run {
                when {
                    this == VerificationStatus.INITIATED -> {
                        onVerificationNumberStarting()
                    }
                    this == VerificationStatus.SUCCESS -> {
                        onVerificationNumberCompleted()
                    }
                    this == VerificationStatus.FAILED -> {
                        onVerificationNumberFailed()
                    }
                    this == VerificationStatus.USER_DENY -> {
                        onVerificationPermissionDeny()
                    }
                    else -> return@Observer
                }

            }
        })
        viewModel.signUpStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                SignUpStepStatus.ERROR -> {
                    hideProgress()
                    showToast(requireContext().getString(R.string.generic_message_for_error))
                }
                else -> return@Observer
            }
        })
    }

    private fun startProgress() {
        binding.btnLogin.showProgress {
            progressColors =
                intArrayOf(ContextCompat.getColor(requireContext(), R.color.text_color_10))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnLogin.isEnabled = false
    }

    private fun hideProgress() {
        updateLoginButtonText()
        binding.btnLogin.isEnabled = true
        binding.btnLogin.hideProgress(getButtonText())
    }

    private fun getButtonText(): Int {
        return if (verificationVia == VerificationVia.FLASH_CALL) {
            R.string.missed_call_label
        } else {
            R.string.send_otp
        }
    }


    private fun updateLoginButtonText() {
        if (verificationVia == VerificationVia.FLASH_CALL) {
            binding.btnLogin.setText(R.string.missed_call_label)
            binding.info.text = getString(R.string.missed_call_verify_message)
        } else {
            binding.btnLogin.setText(R.string.send_otp)
            binding.info.text = getString(R.string.otp_verify_message)
        }
    }

    private fun enableMobileEditText() {
        binding.mobileEt.isFocusableInTouchMode = true
        binding.mobileEt.isEnabled = true
        binding.btnLogin.isEnabled = true
    }

    private fun disableMobileEditText() {
        binding.mobileEt.isFocusableInTouchMode = false
        binding.mobileEt.isEnabled = false
        binding.btnLogin.isEnabled = false

    }

    fun loginViaTrueCaller() {
        viewModel.loginAnalyticsEvent(LoginViaStatus.TRUECALLER.name)
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.TRUECALLER))
    }

    fun loginViaGoogle() {
        viewModel.loginAnalyticsEvent(LoginViaStatus.GMAIL.name)
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.GMAIL))
    }

    fun loginViaFacebook() {
        viewModel.loginAnalyticsEvent(LoginViaStatus.FACEBOOK.name)
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.FACEBOOK))
    }

    fun loginViaPhoneNumber() {
        if (binding.mobileEt.text.isNullOrEmpty() || isValidFullNumber(
                prefix,
                binding.mobileEt.text.toString()
            ).not()
        ) {
            showToast(getString(R.string.please_enter_valid_number))
            return
        }
        startProgress()
        hideKeyboard(requireActivity(), binding.mobileEt)
        evaluateVerificationService()
    }

    fun clearPhoneNumber() {
        binding.mobileEt.setText(EMPTY)
    }

    private fun setupVerificationSystem(countryRegion: String? = null) {
        var defaultRegion: String = countryRegion ?: EMPTY
        if (defaultRegion.isEmpty()) {
            defaultRegion = PhoneNumberUtils.getDefaultCountryIso(requireContext())
        }
        verificationVia = if (defaultRegion == "IN") {
            VerificationVia.SMS
        } else {
            VerificationVia.FLASH_CALL
        }
        updateLoginButtonText()
    }

    private fun evaluateVerificationService() {
        val defaultRegion: String = getCountryIsoCode(
            prefix.plus(binding.mobileEt.text!!.toString()),
            prefix
        )
        verificationService = if (defaultRegion == "IN") {
            VerificationService.SMS_COUNTRY
        } else {
            VerificationService.SINCH
        }
        callVerificationService()
        disableMobileEditText()
    }

    private fun callVerificationService() {
        (requireActivity() as SignUpActivity).createVerification(
            prefix,
            binding.mobileEt.text!!.toString(),
            service = verificationService,
            verificationVia = verificationVia
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }


    private fun startVerificationTimer() {
        binding.progressBar.max = TIMEOUT_TIME.toInt()
        binding.progressBar.progress = lastTime
        timer = object : CountDownTimer(TIMEOUT_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                lastTime = millisUntilFinished.toInt()
                if (timer != null && isAdded && isVisible) {
                    AppObjectController.uiHandler.post {
                        binding.progressBar.progress = millisUntilFinished.toInt()
                        binding.timerTv.text = getString(
                            R.string.wait_for_second,
                            String.format(
                                "00:%02d",
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                            )
                        )
                    }
                }
            }

            override fun onFinish() {
                if (isAdded && isVisible) {
                    AppObjectController.uiHandler.post {
                        binding.progressBarGroup.visibility = View.GONE
                    }
                    onVerificationNumberFailed()
                }
            }
        }
        timer?.start()
    }

    private fun onVerificationNumberStarting() {
        binding.progressBarGroup.visibility = View.VISIBLE
        binding.btnLogin.visibility = View.INVISIBLE
        startVerificationTimer()
    }

    private fun onVerificationNumberFailed() {
        if (isResumed) {
            timer?.cancel()
            timer = null
            AppObjectController.uiHandler.post {
                if (isAdded && isVisible) {
                    binding.btnLogin.visibility = View.VISIBLE
                    binding.progressBar.progress = 0
                    binding.timerTv.text = EMPTY
                    binding.progressBarGroup.visibility = View.GONE
                    enableMobileEditText()
                    flashCallVerificationFailed()
                }
            }
        }
    }

    private fun onVerificationNumberCompleted() {
        RxBus2.publish(
            LoginViaEventBus(
                LoginViaStatus.NUMBER_VERIFY,
                prefix,
                binding.mobileEt.text!!.toString()
            )
        )
        if (isResumed) {
            timer?.cancel()
            binding.btnLogin.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.timerTv.text = EMPTY
            binding.progressBarGroup.visibility = View.GONE
        }
    }

    override fun retryVerificationThrowFlashCall() {
        verificationVia = VerificationVia.FLASH_CALL
        retryVerification()
    }

    override fun retryVerificationThrowSms() {
        verificationVia = VerificationVia.SMS
        retryVerification()
    }

    private fun onVerificationPermissionDeny() {
        hideProgress()
        onVerificationNumberFailed()
    }

    private fun retryVerification() {
        updateLoginButtonText()
        callVerificationService()
        disableMobileEditText()
    }
}
