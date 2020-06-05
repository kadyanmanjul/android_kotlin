package com.joshtalks.joshskills.ui.signup_v2

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
import com.github.razir.progressbutton.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentSignUpOptionsBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.ui.signup.DEFAULT_COUNTRY_CODE
import com.sinch.verification.PhoneNumberUtils
import java.util.concurrent.TimeUnit

class SignUpOptionsFragment : BaseSignUpFragment() {

    private lateinit var viewModel: SignUpV2ViewModel
    private lateinit var binding: FragmentSignUpOptionsBinding
    private var timer: CountDownTimer? = null
    private var verificationVia: VerificationVia = VerificationVia.FLASH_CALL
    private var verificationService: VerificationService = VerificationService.TRUECALLER
    private var lastTime = 0

    companion object {
        fun newInstance() = SignUpOptionsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpV2ViewModel::class.java)
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
        setupVerificationSystem()
        if (Utils.isTrueCallerAppExist()) {
            binding.btnTruecallerLogin.visibility = View.VISIBLE
        }
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setCountryForNameCode(DEFAULT_COUNTRY_CODE)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        binding.mobileEt.prefix = binding.countryCodePicker.defaultCountryCodeWithPlus
        binding.countryCodePicker.setOnCountryChangeListener {
            binding.mobileEt.prefix = binding.countryCodePicker.selectedCountryCodeWithPlus
            setupVerificationSystem(binding.countryCodePicker.selectedCountryNameCode)
        }
        val supportedCountryList =
            AppObjectController.getFirebaseRemoteConfig().getString("SUPPORTED_COUNTRY_LIST")
        if (supportedCountryList.isNotEmpty()) {
            binding.countryCodePicker.setCustomMasterCountries(supportedCountryList)
        }
        bindProgressButton(binding.btnLogin)
        binding.btnLogin.attachTextChangeAnimator()
        viewModel.verificationStatus.observe(this, Observer {
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
                    this == VerificationStatus.TIMEOUT -> {

                    }
                    else -> {

                    }
                }

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
        binding.btnLogin.isEnabled = true
        binding.btnLogin.hideProgress()
        updateLoginButtonText()
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
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.TRUECALLER))
    }

    fun loginViaGoogle() {
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.GMAIL))
    }

    fun loginViaFacebook() {
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.FACEBOOK))
    }

    fun loginViaPhoneNumber() {
        if (binding.mobileEt.text.isNullOrEmpty() || isValidFullNumber(
                binding.mobileEt.prefix,
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
        val defaultRegion: String = PhoneNumberUtils.getDefaultCountryIso(requireContext())
        verificationService = if (defaultRegion == "IN") {
            VerificationService.SMS_COUNTRY
            /*if (VerificationVia.FLASH_CALL == verificationVia) {
                VerificationService.TRUECALLER
            } else {
                VerificationService.SMS_COUNTRY
            }*/
        } else {
            VerificationService.SINCH
        }

        callVerificationService()
        disableMobileEditText()
    }

    private fun callVerificationService() {
        (requireActivity() as SignUpV2Activity).createVerification(
            binding.mobileEt.prefix,
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
        binding.btnLogin.visibility = View.GONE
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
                binding.mobileEt.prefix,
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
        updateLoginButtonText()
        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_FLASH_PARAM.NAME)
            .push()
    }

    override fun retryVerificationThrowSms() {
        verificationVia = VerificationVia.SMS
        updateLoginButtonText()
    }

    private fun onVerificationPermissionDeny() {
        hideProgress()
        onVerificationNumberFailed()
    }
}
