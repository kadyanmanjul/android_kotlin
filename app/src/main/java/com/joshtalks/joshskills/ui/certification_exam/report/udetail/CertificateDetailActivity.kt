package com.joshtalks.joshskills.ui.certification_exam.report.udetail

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.auth.api.credentials.IdentityProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.DATE_FORMATTER
import com.joshtalks.joshskills.core.DATE_FORMATTER_2
import com.joshtalks.joshskills.core.EMAIL_HINT
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.MAX_YEAR
import com.joshtalks.joshskills.core.RC_HINT
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCertificateDetailBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationUserDetail
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val REPORT_ID = "report_id"
const val COUNTRY_CODE = "+91"
const val CERTIFICATE_URL = "certificate_url"

class CertificateDetailActivity : BaseActivity() {

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private lateinit var binding: ActivityCertificateDetailBinding
    private var datePicker: DatePickerDialog? = null
    private var userDateOfBirth: String = EMPTY
    private var mobileHintShowing = false
    private var mCredentialsApiClient: CredentialsClient? = null
    private var emailHintShowing = false
    private var isPostalRequire = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_certificate_detail)
        binding.lifecycleOwner = this
        binding.handler = this
        initDOBPicker()
        initView()
        addObserver()
        viewModel.getCertificateUserDetails()
    }

    private fun initView() {
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                this@CertificateDetailActivity.finish()
            }
        }
        findViewById<AppCompatTextView>(R.id.text_message_title).text = "Certificate Details"

        mCredentialsApiClient = Credentials.getClient(this)
        binding.etMobile.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                requestHint()
        }
        binding.etEmail.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                emailChooser()
        }
    }

    private fun getReportId(): Int {
        return intent.getIntExtra(REPORT_ID, -1)
    }

    private fun initDOBPicker() {
        val now = Calendar.getInstance()
        val minYear = now.get(Calendar.YEAR) - 99
        val maxYear = now.get(Calendar.YEAR) - MAX_YEAR
        datePicker = SpinnerDatePickerDialogBuilder()
            .context(this)
            .callback { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                binding.etDob.setText(DATE_FORMATTER_2.format(calendar.time))
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

    private fun requestHint() {
        if (mobileHintShowing.not()) {
            mobileHintShowing = true
            val hintRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .setEmailAddressIdentifierSupported(false)
                .build()
            val options = CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build()
            val pendingIntent = Credentials.getClient(application, options)
                .getHintPickerIntent(hintRequest)
            try {
                startIntentSenderForResult(pendingIntent.intentSender, RC_HINT, null, 0, 0, 0)
            } catch (e: IntentSender.SendIntentException) {
                mobileHintShowing = true
            }
        }
    }

    private fun emailChooser() {
        if (emailHintShowing.not()) {
            emailHintShowing = true
            val hintRequest = HintRequest.Builder()
                .setHintPickerConfig(
                    CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build()
                )
                .setEmailAddressIdentifierSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build()

            val intent: PendingIntent? = mCredentialsApiClient?.getHintPickerIntent(hintRequest)
            try {
                startIntentSenderForResult(intent?.intentSender, EMAIL_HINT, null, 0, 0, 0)
            } catch (e: IntentSender.SendIntentException) {
                emailHintShowing = true
                e.printStackTrace()
            }
        }
    }

    private fun addObserver() {
        viewModel.apiStatus.observe(
            this,
            {
                hideProgressBar()
            }
        )
        lifecycleScope.launchWhenCreated {
            viewModel.certificateUrl.collectLatest {
                val resultIntent = Intent().apply {
                    putExtra(REPORT_ID, getReportId())
                    putExtra(CERTIFICATE_URL, it)
                }
                setResult(RESULT_OK, resultIntent)
                this@CertificateDetailActivity.finish()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.cUserDetails.collectLatest {
                it?.let {
                    binding.obj = it
                    if (it.isPostalRequire) {
                        binding.tvPoAdd.visibility = View.VISIBLE
                        binding.tvPoSubAdd.visibility = View.VISIBLE
                        binding.etPostal.visibility = View.VISIBLE
                        isPostalRequire = true
                    }
                    binding.etPostal.setText(it.postalAddress)
                    binding.etMotherName.setText(it.motherName)
                    binding.etFatherName.setText(it.fatherName)
                    binding.etMobile.setText(getMobileNumber(it.mobile))
                    binding.etEmail.setText(it.email)
                    binding.etName.setText(it.fullName)
                    binding.etName.requestFocus()
                    binding.etName.setSelection(it.fullName?.length ?: 0)

                    if (it.dateOfBirth.isNullOrEmpty().not()) {
                        userDateOfBirth = it.dateOfBirth ?: EMPTY
                        binding.etDob.setText(getFormatDate(it.dateOfBirth!!))
                    }
                }
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun getFormatDate(date: String): String {
        try {
            val date: Date? = DATE_FORMATTER.parse(date)
            return DATE_FORMATTER_2.format(date)
        } catch (ex: ParseException) {
            ex.printStackTrace()
        } catch (ex: NullPointerException) {
            ex.printStackTrace()
        }
        return EMPTY
    }

    private fun getMobileNumber(mobile: String?): String {
        if (mobile.isNullOrEmpty()) {
            return EMPTY
        }
        return mobile.replace(COUNTRY_CODE, EMPTY)
    }

    private fun isValidIndianNumber(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrEmpty()) {
            return false
        }
        return Pattern.compile("[6-9][0-9]{9}").matcher(phoneNumber).matches()
    }

    private fun isEmailValid(email: String?): Boolean {
        if (email.isNullOrEmpty()) {
            return false
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun selectDateOfBirth() {
        datePicker?.show()
    }

    fun submit() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (binding.etName.text.isNullOrEmpty()) {
                showToast(getString(R.string.name_error_toast))
                return@launch
            }
            if (binding.etDob.text.isNullOrEmpty()) {
                showToast(getString(R.string.dob_error_toast))
                return@launch
            }
            if (binding.etMotherName.text.isNullOrEmpty()) {
                showToast(getString(R.string.mname_error_toast))
                return@launch
            }
            if (binding.etFatherName.text.isNullOrEmpty()) {
                showToast(getString(R.string.fname_error_toast))
                return@launch
            }
            if (isValidIndianNumber(binding.etMobile.text.toString()).not()) {
                showToast(getString(R.string.please_enter_valid_number))
                return@launch
            }
            if (isEmailValid(binding.etEmail.text.toString()).not()) {
                showToast(getString(R.string.enter_valid_email_toast))
                return@launch
            }
            showProgressBar()
            viewModel.postCertificateUserDetails(getUserDetail())
        }
    }

    private fun getUserDetail(): CertificationUserDetail {
        return CertificationUserDetail(
            fullName = this.binding.etName.text.toString(),
            reportId = this.getReportId(),
            dateOfBirth = this.userDateOfBirth,
            email = this.binding.etEmail.text.toString(),
            mobile = COUNTRY_CODE + this.binding.etMobile.text.toString(),
            motherName = this.binding.etMotherName.text.toString(),
            fatherName = this.binding.etFatherName.text.toString(),
            isPostalRequire = this.isPostalRequire,
            postalAddress = this.binding.etPostal.text.toString(),
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == RC_HINT) {
                    val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)
                    binding.etMobile.setText(credential?.id?.replaceFirst(COUNTRY_CODE, EMPTY))
                } else if (requestCode == EMAIL_HINT) {
                    val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)
                    binding.etEmail.setText(credential?.id)
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    companion object {
        fun startUserDetailsActivity(
            context: Context,
            rId: Int = -1,
            conversationId: String? = null,
        ): Intent {
            return Intent(context, CertificateDetailActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(REPORT_ID, rId)
            }
        }
    }
}
