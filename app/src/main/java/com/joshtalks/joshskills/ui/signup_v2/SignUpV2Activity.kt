package com.joshtalks.joshskills.ui.signup_v2

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.facebook.*
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sinch.verification.*
import com.truecaller.android.sdk.*
import com.truecaller.android.sdk.clients.VerificationCallback
import com.truecaller.android.sdk.clients.VerificationDataBundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

private const val GOOGLE_SIGN_UP_REQUEST_CODE = 9001
const val FLOW_FROM = "Flow"

class SignUpV2Activity : BaseActivity() {

    private lateinit var appAnalytics: AppAnalytics
    private val viewModel: SignUpV2ViewModel by lazy {
        ViewModelProvider(this).get(SignUpV2ViewModel::class.java)
    }
    private lateinit var binding: ActivitySignUpV2Binding
    private var fbCallbackManager = CallbackManager.Factory.create()
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var compositeDisposable = CompositeDisposable()
    var verification: Verification? = null
    private var sinchConfig: Config? = null


    init {
        sinchConfig = SinchVerification.config()
            .applicationKey(BuildConfig.SINCH_API_KEY)
            .appHash(AppSignatureHelper(AppObjectController.joshApplication).appSignatures[0])
            .context(AppObjectController.joshApplication)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appAnalytics = AppAnalytics.create(AnalyticsEvent.SIGNUP_SATUS.NAME)
            .addBasicParam()
            .addUserDetails()
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(FLOW_FROM))
            appAnalytics.addParam(
                AnalyticsEvent.FLOW_FROM_PARAM.NAME,
                intent.getStringExtra(FLOW_FROM)
            )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up_v2)
        binding.handler = this
        addViewModelObserver()
        initLoginFeatures()
        setupTrueCaller()
        if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
            openSignUpOptionsFragment()
        } else {
            openProfileDetailFragment()
        }
    }

    private fun addViewModelObserver() {
        viewModel.signUpStatus.observe(this, Observer {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.RequestForOTP -> {
                    openNumberVerificationFragment()
                }
                SignUpStepStatus.ProfileInCompleted -> {
                    openProfileDetailFragment()
                }
                SignUpStepStatus.ProfileCompleted, SignUpStepStatus.SignUpCompleted -> {
                    appAnalytics.addParam(
                        AnalyticsEvent.STATUS.NAME,
                        AnalyticsEvent.SUCCESS_PARAM.NAME
                    )
                    startActivity(getInboxActivityIntent())
                    this@SignUpV2Activity.finishAffinity()
                }
                else -> return@Observer
            }
        })
        viewModel.progressBarStatus.observe(this, Observer {
            showProgressBar()
        })
    }

    private fun initLoginFeatures() {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            setupGoogleLogin()
            setupFacebookLogin()
        }
    }

    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestId().requestProfile().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()
    }

    private fun setupFacebookLogin() {
        LoginManager.getInstance().loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
        LoginManager.getInstance().registerCallback(fbCallbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    // Toast.makeText(applicationContext,"onSuccess"+loginResult?.accessToken,Toast.LENGTH_LONG).show()
                    if (loginResult != null && loginResult.accessToken != null) {
                        getUserDetailsFromFB(loginResult.accessToken)
                    }
                }

                override fun onCancel() {
                    // Toast.makeText(applicationContext,"onCancel",Toast.LENGTH_LONG).show()

                }

                override fun onError(exception: FacebookException) {
                    exception.printStackTrace()
                    // Toast.makeText(applicationContext,"error"+exception.message+" "+ exception.localizedMessage,Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun setupTrueCaller() {
        val trueScope = TruecallerSdkScope.Builder(this, object : ITrueCallback {
            override fun onFailureProfileShared(trueError: TrueError) {
                hideProgressBar()
            }

            override fun onSuccessProfileShared(trueProfile: TrueProfile) {
                viewModel.verifyUserViaTrueCaller(trueProfile)
            }

            override fun onVerificationRequired() {
            }
        })
            .consentMode(TruecallerSdkScope.CONSENT_MODE_POPUP)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_SKIP)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
            val locale = Locale("en")
            TruecallerSDK.getInstance().setLocale(locale)
        }
    }

    private fun openSignUpOptionsFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpOptionsFragment::class.java.name)
            replace(
                R.id.container,
                SignUpOptionsFragment.newInstance(),
                SignUpOptionsFragment::class.java.name
            )
        }
    }

    private fun openProfileDetailFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileFragment.newInstance(),
                SignUpProfileFragment::class.java.name
            )
        }
    }

    private fun openNumberVerificationFragment() {
        appAnalytics.addParam(AnalyticsEvent.LOGIN_VIA.NAME,AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpVerificationFragment::class.java.name)
            replace(
                R.id.container,
                SignUpVerificationFragment.newInstance(),
                SignUpVerificationFragment::class.java.name
            )
        }
    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_UP_REQUEST_CODE) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                handleGoogleSignInResult(account)
                return
            } else {
                hideProgressBar()
            }
        }
        fbCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().onActivityResultObtained(this, resultCode, data)
        }
    }

    private fun gmailLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.GMAIL_PARAM.NAME)
            .push()

        val signInIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_UP_REQUEST_CODE)
    }

    private fun facebookLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.FACEBOOK_PARAM.NAME)
            .push()
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().logIn(this, listOf("public_profile", "email"))
    }

    private fun trueCallerLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.TRUECALLER_PARAM.NAME)
            .push()
        TruecallerSDK.getInstance().getUserProfile(this@SignUpV2Activity)
    }

    fun getUserDetailsFromFB(accessToken: AccessToken) {
        AppAnalytics.create(AnalyticsEvent.LOGIN_SUCCESSFULLY.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.LOGIN_WITH.NAME,AnalyticsEvent.FACEBOOK_PARAM.NAME)
        val request: GraphRequest = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            val id = jsonObject.getString("id")
            var name: String? = null
            if (jsonObject.has("name")) {
                name = jsonObject.getString("name")
            }
            var email: String? = null
            if (jsonObject.has("email")) {
                email = jsonObject.getString("email")
            }
            viewModel.signUpUsingSocial(
                LoginViaStatus.FACEBOOK,
                id,
                name,
                email,
                getFBProfilePicture(id)
            )
        }
        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        if (account.id.isNullOrEmpty().not()) {
            viewModel.signUpUsingSocial(
                LoginViaStatus.GMAIL,
                account.id!!,
                account.displayName,
                account.email,
                account.photoUrl?.toString()
            )
        }
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(LoginViaEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    when (it.loginViaStatus) {
                        LoginViaStatus.FACEBOOK -> {
                            appAnalytics.addParam(
                                AnalyticsEvent.LOGIN_VIA.NAME,
                                AnalyticsEvent.FACEBOOK_PARAM.NAME
                            )
                            showProgressBar()
                            facebookLogin()
                        }
                        LoginViaStatus.GMAIL -> {
                            appAnalytics.addParam(
                                AnalyticsEvent.LOGIN_VIA.NAME,
                                AnalyticsEvent.GMAIL_PARAM.NAME
                            )
                            showProgressBar()
                            gmailLogin()
                        }
                        LoginViaStatus.TRUECALLER -> {
                            appAnalytics.addParam(
                                AnalyticsEvent.LOGIN_VIA.NAME,
                                AnalyticsEvent.TRUECALLER_PARAM.NAME
                            )
                            showProgressBar()
                            trueCallerLogin()
                        }
                        LoginViaStatus.NUMBER_VERIFY -> {
                            viewModel.signUpAfterPhoneVerify(it.countryCode, it.mNumber)
                        }
                        else -> {
                            appAnalytics.addParam(
                                AnalyticsEvent.LOGIN_VIA.NAME,
                                AnalyticsEvent.SMS_OTP_PARAM.NAME
                            )
                            AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
                                .addBasicParam()
                                .addUserDetails()
                                .addParam(
                                    AnalyticsEvent.LOGIN_VIA.NAME,
                                    AnalyticsEvent.SMS_OTP_PARAM.NAME
                                )
                                .push()
                            viewModel.signUpUsingSMS(it.countryCode, it.mNumber)
                        }
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun showProgressBar() {
        FullScreenProgressDialog.showProgressBar(this)
    }

    private fun hideProgressBar() {
        FullScreenProgressDialog.hideProgressBar(this)
    }

    override fun onBackPressed() {
        supportFragmentManager.popBackStackImmediate()
        if (supportFragmentManager.backStackEntryCount == 0) {
            this@SignUpV2Activity.finish()
            return
        }
    }

    fun createVerification(
        countryCode: String,
        phoneNumber: String,
        service: VerificationService = VerificationService.SINCH,
        verificationVia: VerificationVia = VerificationVia.FLASH_CALL
    ) {
        when (service) {
            VerificationService.SINCH -> {
                AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.SINCH_PARAM.NAME)
                    .push()
                verificationThroughSinch(countryCode, phoneNumber, verificationVia)
            }
            VerificationService.TRUECALLER -> {
                verificationThroughTrueCaller(phoneNumber, verificationVia)
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

    //Use link = https://developers.sinch.com/docs/verification-for-android
    private fun verificationThroughSinch(
        countryCode: String,
        phoneNumber: String,
        verificationVia: VerificationVia
    ) {
        val listener = object : VerificationListener {
            override fun onInitiationFailed(e: Exception) {
                viewModel.verificationStatus.postValue(VerificationStatus.FAILED)
                e.printStackTrace()
                when (e) {
                    is InvalidInputException -> {
                    }
                    is ServiceErrorException -> {
                    }
                    else -> {
                    }
                }
            }

            override fun onVerified() {
                viewModel.verificationStatus.postValue(VerificationStatus.SUCCESS)
            }

            override fun onInitiated(p0: InitiationResult) {
                viewModel.verificationStatus.postValue(VerificationStatus.INITIATED)
            }

            override fun onVerificationFailed(e: Exception) {
                e.printStackTrace()
                when (e) {
                    is InvalidInputException -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.FAILED)
                        // Incorrect number or code provided
                    }
                    is CodeInterceptionException -> {
                        // Intercepting the verification code automatically failed, input the code manually with verify()
                    }
                    is IncorrectCodeException -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.FAILED)

                        // The verification code provided was incorrect
                    }
                    is ServiceErrorException -> {
                        viewModel.verificationStatus.postValue(VerificationStatus.FAILED)
                        // Sinch service error
                    }
                    else -> {
                        // Other system error, such as UnknownHostException in case of network error
                    }
                }
                e.printStackTrace()
            }

            override fun onVerificationFallback() {
            }
        }

        val defaultRegion: String = PhoneNumberUtils.getDefaultCountryIso(this)
        val phoneNumberInE164: String =
            PhoneNumberUtils.formatNumberToE164(phoneNumber, defaultRegion)

        if (verificationVia == VerificationVia.FLASH_CALL) {
            flashCallVerificationPermissionCheck {
                verificationViaFLASHCallUsingSinch(sinchConfig, phoneNumberInE164, listener)
            }
        } else {
            verificationViaSMSUsingSinch(
                sinchConfig,
                countryCode,
                phoneNumber,
                phoneNumberInE164,
                listener
            )
        }
    }

    private fun verificationViaSMSUsingSinch(
        config: Config?,
        countryCode: String,
        phoneNumber: String,
        phoneNumberInE164: String,
        listener: VerificationListener
    ) {
        verification =
            SinchVerification.createSmsVerification(config, phoneNumberInE164, listener)
        verification?.initiate()
        viewModel.countryCode = countryCode
        viewModel.phoneNumber = phoneNumber
        openNumberVerificationFragment()
        viewModel.registerSMSReceiver()
    }

    private fun verificationViaFLASHCallUsingSinch(
        config: Config?,
        phoneNumberInE164: String,
        listener: VerificationListener
    ) {
        verification =
            SinchVerification.createFlashCallVerification(config, phoneNumberInE164, listener)
        verification?.initiate()
    }

    //Use link = https://docs.truecaller.com/truecaller-sdk/android/integrating-with-your-app/verifying-non-truecaller-users
    private fun verificationThroughTrueCaller(
        phoneNumber: String,
        verificationVia: VerificationVia
    ) {
        val apiCallback: VerificationCallback = object : VerificationCallback {
            override fun onRequestSuccess(
                requestCode: Int,
                @Nullable extras: VerificationDataBundle?
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
                    VerificationCallback.TYPE_PROFILE_VERIFIED_BEFORE -> {
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
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
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
                                this@SignUpV2Activity,
                                R.string.flash_call_verify_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    viewModel.verificationStatus.postValue(VerificationStatus.USER_DENY)
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        appAnalytics.push()
        super.onDestroy()
    }
}

