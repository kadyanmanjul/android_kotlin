package com.joshtalks.joshskills.ui.signup

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.flurry.sdk.it
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.truecaller.android.sdk.*
import com.truecaller.android.sdk.clients.VerificationCallback
import com.truecaller.android.sdk.clients.VerificationDataBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private var prefix: String = "+91"
    lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }
    private lateinit var auth: FirebaseAuth
    private var verificationVia: VerificationVia = VerificationVia.SMS
    private var verificationService: VerificationService = VerificationService.SMS_COUNTRY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_on_board
        )
        layout.handler = this
        layout.lifecycleOwner = this
        addViewModelObserver()
        initLoginFeatures()
        initTrueCallerUI()

        if (User.getInstance().isVerified ) {
            openProfileDetailFragment(false)
        } else {
            openSignUpOptionsFragment()
        }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN)
    }

    fun signUp() {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
                .push()
            val intent = Intent(this@FreeTrialOnBoardActivity, SignUpActivity::class.java).apply {
                putExtra(FLOW_FROM, "free trial onboarding journey")
            }
            startActivity(intent)
        }
    }

    fun showStartTrialPopup() {
        viewModel.saveImpression(IMPRESSION_START_FREE_TRIAL)
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.START_NOW_CLICKED.value)
        layout.btnStartTrial.pauseAnimation()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.freetrial_alert_dialog, null)
        dialogBuilder.setView(dialogView)
        MarketingAnalytics.startFreeTrail()
        val alertDialog: AlertDialog = dialogBuilder.create()
        val width = AppObjectController.screenWidth * .9
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        alertDialog.show()
        alertDialog.window?.setLayout(width.toInt(), height)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.e_g_motivat).text =
            getString(R.string.free_trial_dialog_desc).replace("\\n", "\n")
        dialogView.findViewById<MaterialTextView>(R.id.yes).setOnClickListener {
            if (Mentor.getInstance().getId().isNotEmpty()) {
                viewModel.saveImpression(IMPRESSION_START_TRIAL_YES)
                PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.JI_HAAN_CLICKED.value)
                openProfileDetailFragment()
                alertDialog.dismiss()
            }
        }

        dialogView.findViewById<MaterialTextView>(R.id.cancel).setOnClickListener {
            viewModel.saveImpression(IMPRESSION_START_TRIAL_NO)
            alertDialog.dismiss()
        }
    }

    private fun openProfileDetailFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
        }
    }
    private fun openProfileDetailFragment(isRegistrationScreenFirstTime: Boolean) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(isRegistrationScreenFirstTime),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
        }
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            this@FreeTrialOnBoardActivity.finish()
            return
        }
        super.onBackPressed()
    }
    fun trueCaller() {
//        showProgressBar()
        if (TruecallerSDK.getInstance().isUsable()) {
            TruecallerSDK.getInstance().getUserProfile(this@FreeTrialOnBoardActivity)
        } else { //else open the ji haan pop up
            showStartTrialPopup()
        }
//        TruecallerSDK.getInstance().getUserProfile(this@FreeTrialOnBoardActivity)
//        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    fun initTrueCallerUI() {
        val trueScope = TruecallerSdkScope.Builder(this, sdkCallback)
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()

        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
            val locale = Locale(PrefManager.getStringValue(USER_LOCALE))
            TruecallerSDK.getInstance().setLocale(locale)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TruecallerSDK.SHARE_PROFILE_REQUEST_CODE) {
            TruecallerSDK.getInstance()
                .onActivityResultObtained(this, requestCode, resultCode, data)
        }
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().onActivityResultObtained(this, requestCode, resultCode, data)
            return
        }
        hideProgressBar()
    }

    private val sdkCallback: ITrueCallback = object : ITrueCallback {

        override fun onFailureProfileShared(trueError: TrueError) {
            hideProgressBar()
            if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                showToast(application.getString(R.string.internet_not_available_msz))
            }
        }

        override fun onVerificationRequired(p0: TrueError?) {
        }

        override fun onSuccessProfileShared(trueProfile: TrueProfile) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.verifyUserTrueCaller(trueProfile)
            }
        }
    }
    private fun addViewModelObserver() {
        viewModel.signUpStatus.observe(this, androidx.lifecycle.Observer {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.RequestForOTP -> {
                    openNumberVerificationFragment()
                }
                SignUpStepStatus.ProfileInCompleted -> {
//                    binding.ivBack.visibility = View.GONE
                    lifecycleScope.launch(Dispatchers.IO) {
                        PrefManager.clearDatabase()
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
                    }
                    openProfileDetailFragment(true)
                }
                SignUpStepStatus.ProfileCompleted -> {
                    showStartTrialPopup()//ji haan
                }
                SignUpStepStatus.StartAfterPicUploaded, SignUpStepStatus.ProfilePicSkipped, SignUpStepStatus.SignUpCompleted -> {
//                  startActivity(getSignUpProfileForFreeTrialFragmentIntent())
                    showStartTrialPopup()
                    this@FreeTrialOnBoardActivity.finishAffinity()
                }
                else -> return@Observer
            }
        })
        viewModel.progressBarStatus.observe(this, androidx.lifecycle.Observer {
            showProgressBar()
        })
    }
    private fun openNumberVerificationFragment() {
//        appAnalytics.addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpVerificationFragment::class.java.name)
            replace(
                R.id.container,
                SignUpVerificationFragment.newInstance(),
                SignUpVerificationFragment::class.java.name
            )
        }
    }
    private fun initLoginFeatures() {
        auth = FirebaseAuth.getInstance()
    }
    private fun openSignUpOptionsFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
        }
    }
    fun createVerification(
        countryCode: String,
        phoneNumber: String,
        service: VerificationService = VerificationService.SMS_COUNTRY,
        verificationVia: VerificationVia = VerificationVia.SMS,
    ) {

        when (service) {
            VerificationService.SINCH -> {
                // verificationThroughSinch(countryCode, phoneNumber, verificationVia)
            }
            VerificationService.TRUECALLER -> {
                verificationThroughTrueCaller(phoneNumber)
            }
            else -> {
                RxBus2.publish(
                    LoginViaEventBus(
                        LoginViaStatus.SMS_VERIFY,
                        countryCode,
                        phoneNumber
                    )
                )
            }
        }
    }
    private fun verificationThroughTrueCaller(
        phoneNumber: String,
    ) {
        val apiCallback: VerificationCallback = object : VerificationCallback {
            @SuppressLint("SwitchIntDef")
            override fun onRequestSuccess(
                requestCode: Int,
                @Nullable extras: VerificationDataBundle?,
            ) {
                when (requestCode) {
                    VerificationCallback.TYPE_MISSED_CALL_INITIATED -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.INITIATED)
                    }
                    VerificationCallback.TYPE_MISSED_CALL_RECEIVED -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.SUCCESS)
                    }
                    VerificationCallback.TYPE_OTP_INITIATED -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.INITIATED)
                    }
                    VerificationCallback.TYPE_OTP_RECEIVED -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.SUCCESS)

                    }
                    VerificationCallback.TYPE_VERIFICATION_COMPLETE -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.SUCCESS)
                    }
                }
            }

            override fun onRequestFailure(requestCode: Int, @NonNull e: TrueException) {
                viewModel.verificationStatus.postValue(VerificationStatus.FAILED)
            }
        }

        flashCallVerificationPermissionCheck {
            TruecallerSDK.getInstance().requestVerification("IN", phoneNumber, apiCallback, this)
        }
    }
    private fun flashCallVerificationPermissionCheck(callback: () -> Unit = {}) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
        } else {
            arrayListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE
            )
        }
        Dexter.withContext(this)
            .withPermissions(permission)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            callback()
                            return@let
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            viewModel.verificationStatus.postValue(VerificationStatus.USER_DENY)
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                this@FreeTrialOnBoardActivity,
                                R.string.flash_call_verify_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    token: PermissionToken?,
                ) {
                    viewModel.verificationStatus.postValue(VerificationStatus.USER_DENY)
                    token?.continuePermissionRequest()
                }
            }).check()
    }
//    private fun callVerificationService() {
//        (requireActivity() as SignUpActivity).createVerification(
//            prefix,
//            layout.mobileEt.text!!.toString(),
//            service = verificationService,
//            verificationVia = verificationVia
//        )
//    }
override fun onPause() {
    super.onPause()
//    compositeDisposable.clear()
}
    //tc
    override fun onStop() {
//        appAnalytics.push()
        super.onStop()
    }

    //tc
    override fun onDestroy() {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        TruecallerSDK.clear()
        super.onDestroy()
    }

}