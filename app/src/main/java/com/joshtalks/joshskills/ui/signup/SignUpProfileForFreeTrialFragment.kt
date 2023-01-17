package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_ENTER_NAME_TEXT
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.databinding.FragmentSignUpProfileForFreeTrialBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import java.util.concurrent.TimeUnit

class SignUpProfileForFreeTrialFragment : BaseSignUpFragment() {

    private var prefix: String = EMPTY
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpProfileForFreeTrialBinding
    private var username = User.getInstance().firstName
    private var isUserVerified = User.getInstance().isVerified
    private var isNameEntered = false
    private var verificationVia: VerificationVia = VerificationVia.SMS
    private var verificationService: VerificationService = VerificationService.SMS_COUNTRY
    private var timer: CountDownTimer? = null
    private var lastTime = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_sign_up_profile_for_free_trial,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        binding.nameEditText.requestFocus()
        val defaultRegion: String = getDefaultCountryIso(requireContext())
        setupVerificationSystem(defaultRegion)
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
        initUI()
    }

    private fun initUI() {
//        binding.toolbarLayout.ivBack.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
        binding.textViewName.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FREE_TRIAL_ENTER_NAME_TEXT + PrefManager.getStringValue(FREE_TRIAL_TEST_ID, defaultValue = FREE_TRIAL_DEFAULT_TEST_ID))
        binding.nameEditText.setText(username)
        binding.nameEditText.isEnabled = true

        if(viewModel.abTestRepository.isVariantActive(VariantKeys.MANDATORY_NUMBER_ENABLED)){
            binding.etContainer.visibility = View.VISIBLE
            binding.info.visibility = View.VISIBLE
            binding.btnLogin.visibility = View.VISIBLE
//            binding.progressBar.visibility = View.VISIBLE
            binding.btnLoginName.visibility = View.GONE
        }
    }

    private fun setupVerificationSystem(countryRegion: String? = null) {
        var defaultRegion: String = countryRegion ?: EMPTY
        if (defaultRegion.isEmpty()) {
            defaultRegion = getDefaultCountryIso(requireContext())
        }
        verificationVia = if (defaultRegion == "IN") {
            VerificationVia.SMS
        } else {
            VerificationVia.FLASH_CALL
        }
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

    private fun addObservers() {
        binding.nameEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    submitProfile()
                    true
                }
                else -> false
            }
        }
        viewModel.apiStatus.observe(viewLifecycleOwner) {
            when (it) {
                ApiCallStatus.START -> {
                    startProgress()
                    handleOnBackPressed(true)
                }
                ApiCallStatus.SUCCESS -> {
                    hideProgress()
                    moveToInboxScreen()
                    handleOnBackPressed(false)
                }
                else -> {
                    hideProgress()
                    handleOnBackPressed(false)
                }
            }
        }
    }

    fun submitProfile() {
        if(viewModel.abTestRepository.isVariantActive(VariantKeys.MANDATORY_NUMBER_ENABLED)){
            if (binding.mobileEt.text.isNullOrEmpty() || isValidFullNumber(
                    prefix,
                    binding.mobileEt.text.toString()
                ).not()
            ) {
                showToast(getString(R.string.please_enter_valid_number))
                return
            }
            if (binding.nameEditText.text.isNullOrEmpty()) {
                showToast(getString(R.string.name_error_toast))
                return
            }
            evaluateVerificationService()
        }

        if (binding.nameEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.name_error_toast))
            return
        }
        viewModel.saveTrueCallerImpression(PHONE_NUMBER_SUBMITTED)
        if (Utils.isInternetAvailable().not()){
            showToast(getString(R.string.internet_not_available_msz))
            return
        }
        viewModel.saveTrueCallerImpression(NAME_SUBMITTED)
        handleOnBackPressed(true)
        MarketingAnalytics.completeRegistrationAnalytics(
            false,
            RegistrationMethods.TRUE_CALLER
        )
        activity?.let { hideKeyboard(it, binding.nameEditText) }
        submitForFreeTrial()
        binding.btnLogin.isEnabled = false

        val name = binding.nameEditText.text.toString()
        if(username.isNullOrEmpty()) {
            MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                .addParam(ParamKeys.NAME_ENTERED,true)
                .push()
        }
        if (!username.isNullOrEmpty() && username != name) {
            if (!isNameEntered) {
                MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                    .addParam(ParamKeys.NAME_CHANGED,true)
                    .push()
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_NAMECHANGED)
            }
        } else if(!username.isNullOrEmpty() && username==name) {
            MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                .addParam(ParamKeys.NAME_CHANGED,false)
                .push()
        }
    }

    private fun evaluateVerificationService() {
        val defaultRegion: String = getCountryIsoCode(
            prefix.plus(binding.mobileEt.text!!.toString()),
            prefix
        )
        verificationService = if (defaultRegion == "IN") {
            VerificationService.SMS_COUNTRY
        } else {
            VerificationService.SMS_COUNTRY
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

    fun submitForFreeTrial() {
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = binding.nameEditText.text?.toString() ?: EMPTY
        requestMap["is_free_trial"] = "Y"
        viewModel.completingProfile(requestMap, isUserVerified)
        viewModel.postGoal(GoalKeys.NAME_SUBMITTED)
        viewModel.postGoal(GoalKeys.NAME_SELECTED)
    }

    private fun moveToInboxScreen() {
        AppAnalytics.create(AnalyticsEvent.FREE_TRIAL_ONBOARDING.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .push()
        val intent = Intent(requireActivity(), InboxActivity::class.java).apply {
            putExtra(FLOW_FROM, "free trial onboarding journey")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }


    private fun startProgress() {
        binding.btnLogin.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.pure_white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnLogin.isEnabled = false
    }

    private fun hideProgress() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.hideProgress(R.string.register)
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

    fun clearPhoneNumber() {
        binding.mobileEt.setText(EMPTY)
        enableMobileEditText()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        (activity as BaseActivity).showWebViewDialog(url)
    }

    private fun handleOnBackPressed(enabled: Boolean) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(enabled){
                override fun handleOnBackPressed() {

                }
            })
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
