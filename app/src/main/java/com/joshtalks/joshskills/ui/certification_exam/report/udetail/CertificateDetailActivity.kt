package com.joshtalks.joshskills.ui.certification_exam.report.udetail

import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.credentials.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.core.interfaces.FileDownloadCallback
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.databinding.ActivityCertificateDetailBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationUserDetail
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.constants.CERTIFICATE_EXAM_ID
import com.joshtalks.joshskills.ui.certification_exam.constants.CERTIFICATE_SHARE_FRAGMENT
import com.joshtalks.joshskills.ui.certification_exam.constants.GENERATE_CERTIFICATE_FORM
import com.joshtalks.joshskills.ui.certification_exam.utils.DelayedTypingListener
import com.joshtalks.joshskills.ui.certification_exam.view.CertificateShareFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern

const val REPORT_ID = "report_id"
const val COUNTRY_CODE = "+91"
const val CERTIFICATE_URL = "certificate_url"
const val LOCAL_DOWNLOAD_URL = "local_download_url"
const val CERTI_DETAILS = "Certificate Details"
const val EXAMINATION_CERTI = "Examination Certificate"

class CertificateDetailActivity : BaseActivity(), FileDownloadCallback {

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private lateinit var binding: ActivityCertificateDetailBinding
    private var datePicker: DatePickerDialog? = null
    private var userDateOfBirth: String = EMPTY
    private var mobileHintShowing = false
    private var mCredentialsApiClient: CredentialsClient? = null
    private var emailHintShowing = false
    var progressDialog: ProgressDialog? = null

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
        if (intent.hasExtra(CERTIFICATE_EXAM_ID)) {
            intent.getIntExtra(CERTIFICATE_EXAM_ID, 0).let { viewModel.certificateExamId = it }
        }
        if (intent.hasExtra(CERTIFICATE_URL) && intent.getStringExtra(CERTIFICATE_URL) != null) {
            intent.getStringExtra(CERTIFICATE_URL)?.let { openCertificateShareFragment(it) }
            initView()
        } else {
            initDOBPicker()
            initView()
            addObserver()
            viewModel.getCertificateUserDetails()
        }
        binding.etPinCode.addTextChangedListener(DelayedTypingListener(delayMillis = 500L) {
            if (binding.etPinCode.text?.length == 6) {
                showProgressBar()
                viewModel.getInfoFromPinNumber(binding.etPinCode.text.toString().toInt())
                binding.linearLayoutAddress.visibility = View.VISIBLE
                hideKeyboard(this)
                binding.scrollView.smoothScrollTo(0, binding.btnSubmitDetails.y.toInt())
            }
        })
    }

    private fun initView() {
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                this@CertificateDetailActivity.finish()
            }
        }
        findViewById<AppCompatTextView>(R.id.text_message_title).text = CERTI_DETAILS
        findViewById<AppCompatImageView>(R.id.iv_icon_referral).visibility = View.GONE
        if (intent.hasExtra(CERTIFICATE_URL) && intent.getStringExtra(CERTIFICATE_URL) != null) {
            findViewById<AppCompatTextView>(R.id.text_message_title).text = EXAMINATION_CERTI
            hideProgressBar()
            binding.progressBar.visibility = View.GONE
        } else {
            findViewById<AppCompatTextView>(R.id.text_message_title).text = CERTI_DETAILS
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
            this
        ) {
            if (it == ApiCallStatus.FAILED) {
                hideProgressBar()
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.certificateUrl.collectLatest {
                dismissProgressDialog()
                openCertificateShareFragment(it?: EMPTY)
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.cUserDetails.collectLatest {
                it?.let {
                    binding.obj = it
                    binding.etMotherName.setText(it.motherName)
                    binding.etFatherName.setText(it.fatherName)
                    binding.etMobile.setText(getMobileNumber(it.mobile))
                    binding.etEmail.setText(it.email)
                    binding.etName.setText(it.fullName)
                    binding.etName.requestFocus()
                    binding.etName.setSelection(it.fullName?.length ?: 0)
                    if(it.pinCode.toString().length == 6 ){
                        binding.etPinCode.setText(it.pinCode.toString())
                    }
                    binding.etHouseNum.setText(it.houseNumber)
                    binding.etRoadNameColony.setText(it.roadName)
                    binding.etLandmark.setText(it.landmark)
                    binding.etTownOrCity.setText(it.town)
                    binding.autoCompleteTextViewFirst.setText(it.state)

                    if (it.dateOfBirth.isNullOrEmpty().not()) {
                        userDateOfBirth = it.dateOfBirth ?: EMPTY
                        binding.etDob.setText(getFormatDate(it.dateOfBirth!!))
                    }
                }
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.cityUser.collectLatest {
                hideProgressBar()
                if (it == ERROR) {
                    showToast("Please enter a valid Pin Code")
                } else {
                    binding.etTownOrCity.setText(it)
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.stateUser.collectLatest {
                for (i in 0 until binding.autoCompleteTextViewFirst.adapter.count) { //36 are number of states and UT with us
                    if (binding.autoCompleteTextViewFirst.adapter.getItem(i) == it) {
                        binding.autoCompleteTextViewFirst.setText(
                            binding.autoCompleteTextViewFirst.adapter.getItem(i).toString(),
                            false
                        )
                        break
                    }
                }
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

    fun selectDateOfBirth(view: View) {
        datePicker?.show()
    }

    fun submit(view: View) {
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
            if (binding.etPinCode.text?.length != 6) {
                showToast(getString(R.string.enter_postal_address))
                return@launch
            }
            if (binding.etHouseNum.text.isNullOrEmpty()) {
                showToast(getString(R.string.enter_postal_house))
                return@launch
            }
            if (binding.etRoadNameColony.text.isNullOrEmpty()) {
                showToast(getString(R.string.enter_postal_road))
                return@launch
            }
            if (binding.etTownOrCity.text.isNullOrEmpty()) {
                showToast(getString(R.string.enter_postal_town))
                return@launch
            }
            runOnUiThread {
                showProgressDialog(getString(R.string.generating_certificate))
            }

            if (isInternetAvailable()) {
                viewModel.postCertificateUserDetails(getUserDetail())
                viewModel.saveImpression(GENERATE_CERTIFICATE_FORM)
            }
            hideKeyboard(this@CertificateDetailActivity)
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
            pinCode = this.binding.etPinCode.text.toString().toInt(),
            houseNumber = this.binding.etHouseNum.text.toString(),
            roadName = this.binding.etRoadNameColony.text.toString(),
            landmark = this.binding.etLandmark.text.toString(),
            town = this.binding.etTownOrCity.text.toString(),
            state = this.binding.autoCompleteTextViewFirst.text.toString()
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

    override fun onSuccessDismiss() {
        super.onSuccessDismiss()
        lifecycleScope.launch(Dispatchers.Main) {
            binding.rootView.setBackgroundColor(
                ContextCompat.getColor(
                    this@CertificateDetailActivity,
                    R.color.icon_default
                )
            )
            binding.rootView.removeAllViews()
        }
    }

    override fun webURL(webUrl: String, localUrl: String) {
        super.webURL(webUrl, localUrl)
        val resultIntent = Intent().apply {
            putExtra(REPORT_ID, getReportId())
            putExtra(CERTIFICATE_URL, webUrl)
            putExtra(LOCAL_DOWNLOAD_URL, localUrl)
        }
        setResult(RESULT_OK, resultIntent)
        this@CertificateDetailActivity.finish()
    }

    override fun onCancel() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resultIntent = Intent().apply {
                    putExtra(REPORT_ID, getReportId())
                    putExtra(CERTIFICATE_URL, viewModel.certificateUrl.single())
                }
                setResult(RESULT_OK, resultIntent)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            this@CertificateDetailActivity.finish()
        }
    }

    companion object {
        fun startUserDetailsActivity(
            context: Context,
            rId: Int = -1,
            conversationId: String? = null,
            certificateUrl: String? = null,
            certificateExamId: Int? = null
        ): Intent {
            return Intent(context, CertificateDetailActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(REPORT_ID, rId)
                putExtra(CERTIFICATE_URL, certificateUrl)
                putExtra(CERTIFICATE_EXAM_ID, certificateExamId)
            }
        }
    }
    private fun openCertificateShareFragment(url: String?) {
        try {
            if (url.isNullOrEmpty()){
                showToast(getString(R.string.something_went_wrong))
                return
            }
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                val fragment = viewModel.certificateExamId?.let { CertificateShareFragment.newInstance(url?: EMPTY, certificateExamId = it) }
                if (fragment != null) {
                    replace(R.id.container_frame, fragment, CERTIFICATE_SHARE_FRAGMENT)
                }
            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun showProgressDialog(msg: String) {
        progressDialog = ProgressDialog(this, R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()
}
