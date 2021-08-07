package com.joshtalks.joshskills.ui.signup

import android.content.Intent
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
import com.joshtalks.joshskills.databinding.FragmentSignUpProfileForFreeTrialBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import java.util.*

class SignUpProfileForFreeTrialFragment : BaseSignUpFragment() {

    private var prefix: String = EMPTY
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpProfileForFreeTrialBinding
    private var datePicker: DatePickerDialog? = null
    private var gender: GENDER? = null
    private var genderIdArray: Array<Int> =
        arrayOf(R.id.tv_male_gender, R.id.tv_female_gender, R.id.tv_other_gender)
    private var userDateOfBirth: String? = null

    companion object {
        fun newInstance() = SignUpProfileForFreeTrialFragment()
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
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_profile_for_free_trial, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        initDOBPicker()
        initListener()
        initCountryCodePicker()
    }

    private fun addObservers() {
        viewModel.signUpStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                SignUpStepStatus.ProfileCompleted -> {
                    viewModel.startFreeTrial(Mentor.getInstance().getId())
                }
                else -> {
                    hideProgress()
                    return@Observer
                }
            }
        })
        viewModel.apiStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    hideProgress()
                    moveToInboxScreen()
                }
                else -> {
                    hideProgress()
                }
            }
        })
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

    private fun initCountryCodePicker() {
        binding.countryCodePicker.setAutoDetectedCountry(true)
        binding.countryCodePicker.setDetectCountryWithAreaCode(true)
        prefix = binding.countryCodePicker.defaultCountryCodeWithPlus
        binding.countryCodePicker.setOnCountryChangeListener {
            prefix = binding.countryCodePicker.selectedCountryCodeWithPlus
        }
    }

    private fun initListener() {

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

        if (binding.dobEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.dob_error_toast))
            return
        }

        if (binding.phoneNumberEt.text.isNullOrEmpty() || isValidFullNumber(
                prefix,
                binding.phoneNumberEt.text.toString()
            ).not()
        ) {
            showToast(getString(R.string.please_enter_valid_number))
            return
        }

        if (gender == null) {
            showToast(getString(R.string.select_gender))
            return
        }

        startProgress()
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = binding.nameEditText.text?.toString() ?: EMPTY
        requestMap["date_of_birth"] = userDateOfBirth ?: EMPTY
        requestMap["gender"] = gender?.gValue ?: EMPTY
        val mobNo = binding.phoneNumberEt.text!!.toString()
        if (mobNo.isNullOrBlank().not()) {
            requestMap["mobile"] = mobNo
        }
        viewModel.completingProfile(requestMap, false)
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
