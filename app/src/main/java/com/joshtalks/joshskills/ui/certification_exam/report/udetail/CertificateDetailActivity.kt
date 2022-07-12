package com.joshtalks.joshskills.ui.certification_exam.report.udetail

import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.POSTAL_ADDRESS
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.POSTAL_ADDRESS_SUBHEADING_CERT_FORM
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.DATE_FORMATTER
import com.joshtalks.joshskills.core.DATE_FORMATTER_2
import com.joshtalks.joshskills.core.EMAIL_HINT
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.MAX_YEAR
import com.joshtalks.joshskills.core.RC_HINT
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.core.interfaces.FileDownloadCallback
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCertificateDetailBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationUserDetail
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.constants.*
import com.joshtalks.joshskills.ui.certification_exam.view.CertificateShareFragment
import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

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
    private var isPostalRequire = false
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
        if(intent.hasExtra(CERTIFICATE_EXAM_ID)){
            intent.getIntExtra(CERTIFICATE_EXAM_ID,0).let { viewModel.certificateExamId =it }
        }
        if (intent.hasExtra(CERTIFICATE_URL) && intent.getStringExtra(CERTIFICATE_URL) != null) {
            openCertificateShareFragment(intent.getStringExtra(CERTIFICATE_URL)?: EMPTY)
            initView()
        } else {
            initDOBPicker()
            initView()
            addObserver()
            viewModel.getCertificateUserDetails()
        }
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
        }else {
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

            binding.etPostal.overScrollMode = View.OVER_SCROLL_ALWAYS
            binding.etPostal.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            binding.etPostal.movementMethod = ScrollingMovementMethod.getInstance()
            binding.etPostal.setOnTouchListener { view, motionEvent ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                if (motionEvent.action and MotionEvent.ACTION_UP !== 0 && motionEvent.actionMasked and MotionEvent.ACTION_UP !== 0) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
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
                openCertificateShareFragment(it?: EMPTY)
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.cUserDetails.collectLatest {
                it?.let {
                    binding.obj = it
                    if (it.isPostalRequire) {
                        binding.tvPoAdd.text = AppObjectController.getFirebaseRemoteConfig().getString(POSTAL_ADDRESS)
                        binding.tvPoAdd.visibility = View.VISIBLE
                        binding.tvPoSubAdd.text = AppObjectController.getFirebaseRemoteConfig().getString(POSTAL_ADDRESS_SUBHEADING_CERT_FORM)
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
            if(binding.etPostal.text.isNullOrEmpty()){
                showToast(getString(R.string.enter_postal_address))
                return@launch
            }
            if (isInternetAvailable()) {
                viewModel.postCertificateUserDetails(getUserDetail())
                viewModel.saveImpression(GENERATE_CERTIFICATE_FORM)
            }
            try {
                val view = window.currentFocus
                view?.clearFocus()
                val inputMethodManager = this@CertificateDetailActivity.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view?.windowToken,0)
            }catch (e:Exception){
                e.printStackTrace()
            }

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

    override fun onSuccessDismiss() {
        super.onSuccessDismiss()
        lifecycleScope.launch(Dispatchers.Main) {
            binding.rootView.setBackgroundColor(
                ContextCompat.getColor(
                    this@CertificateDetailActivity,
                    R.color.black
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
            certificateExamId:Int?= null
        ): Intent {
            return Intent(context, CertificateDetailActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(REPORT_ID, rId)
                putExtra(CERTIFICATE_URL,certificateUrl)
                putExtra(CERTIFICATE_EXAM_ID,certificateExamId)
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
    /*fun showProgressDialog(msg: String) {
        progressDialog = ProgressDialog(this, R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()*/
}
