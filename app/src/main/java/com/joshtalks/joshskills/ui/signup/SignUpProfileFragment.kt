package com.joshtalks.joshskills.ui.signup

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.databinding.FragmentSignUpProfileBinding
import com.joshtalks.joshskills.repository.local.model.User
import java.util.*

class SignUpProfileFragment : BaseSignUpFragment() {

    private var prefix: String = EMPTY
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpProfileBinding
    private var datePicker: DatePickerDialog? = null
    private var gender: GENDER? = null
    private var genderIdArray: Array<Int> =
        arrayOf(R.id.tv_male_gender, R.id.tv_female_gender, R.id.tv_other_gender)
    private var userDateOfBirth: String? = null

    companion object {
        const val IS_REGISTRATION_SCREEEN_FIRST_TIME = "is_registration_screen_first_time"
        fun newInstance(isRegistrationScreenFirstTime:Boolean) = SignUpProfileFragment().apply {
            arguments=Bundle().apply {
                putBoolean(IS_REGISTRATION_SCREEEN_FIRST_TIME,isRegistrationScreenFirstTime)
            }
        }
    }
    private var isRegistrationFirstTime: Boolean=true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isRegistrationFirstTime =
                it.getBoolean(IS_REGISTRATION_SCREEEN_FIRST_TIME,true)
        }
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_profile, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.signUpStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                SignUpStepStatus.ERROR -> {
                    hideProgress()
                }
                else -> return@Observer
            }
        })
        initDOBPicker()
        initListener()
        initUI()
        logRegistrationAnalyticsEvent(isRegistrationFirstTime)
    }

    private fun logRegistrationAnalyticsEvent(eventName:Boolean) {
        AppAnalytics.create(AnalyticsEvent.REGISTRATION_STARTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.IS_REGISTRATION_FIRST_TIME.NAME,eventName)
            .push()
    }

    private fun logAnalyticsEvent(eventName:String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun initDOBPicker() {
        val now = Calendar.getInstance()
        val minYear = now.get(Calendar.YEAR) - 99
        val maxYear = now.get(Calendar.YEAR) - MAX_YEAR
        datePicker = SpinnerDatePickerDialogBuilder()
            .context(requireActivity())
            .callback { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                binding.dobEditText.setText(DATE_FORMATTER_2.format(calendar.time))
                userDateOfBirth = DATE_FORMATTER.format(calendar.time)
            }
            .spinnerTheme(R.style.DatePickerStyle)
            .showTitle(true)
            .showDaySpinner(true)
            .defaultDate(
                maxYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .minDate(
                minYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .maxDate(
                maxYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .build()
    }

    private fun initUI() {
        initCountryCodePicker()
        val user = User.getInstance()

        if (user.firstName.isNullOrEmpty().not()) {
            binding.nameEditText.setText(user.firstName)
            binding.nameEditText.isEnabled = false
        }

        if (user.email.isNullOrEmpty().not()) {
            binding.textViewEmail.visibility = View.VISIBLE
            binding.emailEditText.setText(user.email)
            binding.emailEditText.visibility = View.VISIBLE
        }

        if (user.phoneNumber.isNullOrEmpty().not()) {
            val word = user.phoneNumber ?: EMPTY
            val length = word.length
            if (length > 10) {
                binding.phoneNumberEt.setText(word.substring(length - 10))
                binding.etContainer.visibility = View.VISIBLE
                binding.textViewPhone.visibility = View.VISIBLE
                binding.countryCodePicker.isEnabled = false
                binding.countryCodePicker.isClickable = false
                binding.countryCodePicker.setCcpClickable(false)
                val countryCode =
                    word.removePrefix("+").removeSuffix(binding.phoneNumberEt.text.toString())
                binding.countryCodePicker.setCountryForPhoneCode(countryCode.toInt())
            }
        } else if (PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).isNotEmpty()) {
            val mobileNumber = PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).split(SINGLE_SPACE)
            if (mobileNumber.isNullOrEmpty().not()) {
                binding.phoneNumberEt.setText(mobileNumber[1])
                binding.etContainer.visibility = View.VISIBLE
                binding.textViewPhone.visibility = View.VISIBLE
            }
        }

        if (binding.phoneNumberEt.text.isNullOrEmpty() && user.email.isNullOrEmpty()) {
            binding.textViewEmail.visibility = View.VISIBLE
            binding.emailEditText.isFocusableInTouchMode = true
            binding.emailEditText.isClickable = true
            binding.emailEditText.isEnabled = true
            binding.emailEditText.visibility = View.VISIBLE
        }

    }

    private fun initCountryCodePicker() {
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        prefix = binding.countryCodePicker.defaultCountryCodeWithPlus
        binding.countryCodePicker.setOnCountryChangeListener {
            prefix = binding.countryCodePicker.selectedCountryCodeWithPlus
        }

    }

    private fun initListener() {
        binding.emailEditText.setOnClickListener {
            emailSelectionHint()
        }
        val toggleListener = View.OnClickListener {
            disableAllGenderView()
            enableSelectedGenderView(it)
            gender = when (it.id) {
                R.id.tv_male_gender -> {
                    GENDER.MALE
                }
                R.id.tv_female_gender -> {
                    GENDER.FEMALE
                }
                else -> {
                    GENDER.OTHER
                }
            }
        }
        binding.tvMaleGender.setOnClickListener(toggleListener)
        binding.tvFemaleGender.setOnClickListener(toggleListener)
        binding.tvOtherGender.setOnClickListener(toggleListener)
    }

    private fun disableAllGenderView() {
        var textView: MaterialTextView?
        genderIdArray.forEach {
            textView = binding.root.findViewById(it) as MaterialTextView
            textView?.backgroundTintList = null
            textView?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_90
                )
            )
            textView?.setBackgroundResource(R.drawable.mobile_no_bg)
        }
    }

    private fun enableSelectedGenderView(view: View) {
        val textView: MaterialTextView = view as MaterialTextView
        textView.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                R.color.text_color_10
            )
        )
        textView.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.white
            )
        )
    }


    fun selectDateOfBirth() {
        datePicker?.show()
    }

    fun submitProfile() {
        if (binding.nameEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.name_error_toast))
            return
        }

        if (binding.phoneNumberEt.text.isNullOrEmpty() && binding.emailEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.enter_valid_email_toast))
        }

        if (binding.dobEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.dob_error_toast))
            return
        }
        if (gender == null) {
            showToast(getString(R.string.select_gender))
            return
        }
        logAnalyticsEvent(AnalyticsEvent.REGISTRATION_NEXT.NAME)
        startProgress()
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = binding.nameEditText.text?.toString() ?: EMPTY
        if (binding.emailEditText.text.isNullOrEmpty().not()) {
            requestMap["email"] = binding.emailEditText.text?.toString() ?: EMPTY
        }
        requestMap["date_of_birth"] = userDateOfBirth ?: EMPTY
        requestMap["gender"] = gender?.gValue ?: EMPTY
        viewModel.completingProfile(requestMap)
    }

    private fun startProgress() {
        binding.btnLogin.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.white))
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


}