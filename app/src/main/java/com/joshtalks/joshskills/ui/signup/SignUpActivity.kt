package com.joshtalks.joshskills.ui.signup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppSignatureHelper
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.VerificationService
import com.joshtalks.joshskills.core.VerificationStatus
import com.joshtalks.joshskills.core.VerificationVia
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.getFBProfilePicture
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sinch.verification.CodeInterceptionException
import com.sinch.verification.Config
import com.sinch.verification.IncorrectCodeException
import com.sinch.verification.InitiationResult
import com.sinch.verification.InvalidInputException
import com.sinch.verification.PhoneNumberUtils
import com.sinch.verification.ServiceErrorException
import com.sinch.verification.SinchVerification
import com.sinch.verification.Verification
import com.sinch.verification.VerificationListener
import com.truecaller.android.sdk.ITrueCallback
import com.truecaller.android.sdk.TrueError
import com.truecaller.android.sdk.TrueException
import com.truecaller.android.sdk.TrueProfile
import com.truecaller.android.sdk.TruecallerSDK
import com.truecaller.android.sdk.TruecallerSdkScope
import com.truecaller.android.sdk.clients.VerificationCallback
import com.truecaller.android.sdk.clients.VerificationDataBundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

private const val GOOGLE_SIGN_UP_REQUEST_CODE = 9001
const val FLOW_FROM = "Flow"

class SignUpActivity : BaseActivity() {

    private lateinit var appAnalytics: AppAnalytics
    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }
    private lateinit var binding: ActivitySignUpV2Binding
    private var fbCallbackManager = CallbackManager.Factory.create()
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var compositeDisposable = CompositeDisposable()
    var verification: Verification? = null
    private var sinchConfig: Config? = null
    private lateinit var auth: FirebaseAuth


    init {
        sinchConfig = SinchVerification.config()
            .applicationKey(BuildConfig.SINCH_API_KEY)
            .appHash(AppSignatureHelper(AppObjectController.joshApplication).appSignatures[0])
            .context(AppObjectController.joshApplication)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appAnalytics = AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(
                AnalyticsEvent.STATUS.NAME,
                AnalyticsEvent.FAILED_PARAM.NAME
            )
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
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

                    logLoginSuccessAnalyticsEvent(viewModel.loginViaStatus?.toString())
                    startActivity(getInboxActivityIntent())
                    this@SignUpActivity.finishAffinity()
                }
                else -> return@Observer
            }
        })
        viewModel.progressBarStatus.observe(this, Observer {
            showProgressBar()
        })
        viewModel.fromVerifictionScreen.observe(this, Observer {
            if (it)
                addRetryCountAnalytics()
        })
    }

    private fun logLoginSuccessAnalyticsEvent(from: String?) {
        appAnalytics.addParam(
            AnalyticsEvent.STATUS.NAME,
            AnalyticsEvent.SUCCESS_PARAM.NAME
        )
        from.let {
            AppAnalytics.create(AnalyticsEvent.LOGIN_SUCCESSFULLY.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.LOGIN_VIA.NAME, from)
                .push()
        }
    }

    private fun addRetryCountAnalytics() {
        appAnalytics.addParam(
            AnalyticsEvent.INCORRECT_OTP_ATTEMPTS.NAME,
            viewModel.incorrectAttempt
        )
            .addParam(AnalyticsEvent.NO_OF_TIMES_OTP_SEND.NAME, viewModel.resendAttempt)
            .addParam(
                AnalyticsEvent.TIME_TAKEN.NAME.plus("(in ms"),
                System.currentTimeMillis() - viewModel.currentTime
            )
    }

    private fun initLoginFeatures() {
        auth = FirebaseAuth.getInstance()
        setupGoogleLogin()
        setupFacebookLogin()
    }

    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().requestId().requestProfile().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupFacebookLogin() {
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
        LoginManager.getInstance().registerCallback(
            fbCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    if (loginResult.accessToken != null) {
                        getUserDetailsFromFB(loginResult.accessToken)
                    } else {
                        showToast(getString(R.string.something_went_wrong))
                        hideProgressBar()
                    }
                }

                override fun onCancel() {
                    hideProgressBar()
                }

                override fun onError(exception: FacebookException) {
                    exception.printStackTrace()
                    LogException.catchException(exception)
                    hideProgressBar()
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
        appAnalytics.addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
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
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Timber.e("firebaseAuthWithGoogle:" + account.id)
                handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                hideProgressBar()
                if (BuildConfig.DEBUG) {
                    showToast(getString(R.string.gmail_login_error_message))
                }
            } catch (e: Exception) {
                hideProgressBar()
            }
        }
        fbCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().onActivityResultObtained(this, resultCode, data)
            return
        }
        hideProgressBar()
    }

    private fun gmailLogin() {
        val signInIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_UP_REQUEST_CODE)
    }

    private fun facebookLogin() {
        LoginManager.getInstance().logIn(this, listOf("public_profile", "email"))
    }

    private fun trueCallerLogin() {
        TruecallerSDK.getInstance().getUserProfile(this@SignUpActivity)
    }

    fun getUserDetailsFromFB(accessToken: AccessToken) {
        val request: GraphRequest = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            try {
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
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        if (account.idToken.isNullOrEmpty().not()) {
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val accountUser = auth.currentUser
                        handleFirebaseAuth(account.id, accountUser)
                    } else {
                        task.exception?.showAppropriateMsg()

                        showToast(getString(R.string.generic_message_for_error))
                    }
                }
        } else {
            showToast(getString(R.string.generic_message_for_error))
        }
    }

    private fun handleFirebaseAuth(
        gId: String?,
        accountUser: FirebaseUser?
    ) {
        if (accountUser != null) {
            viewModel.signUpUsingSocial(
                LoginViaStatus.GMAIL,
                gId ?: accountUser.uid,
                accountUser.displayName,
                accountUser.email,
                accountUser.photoUrl?.toString()
            )
        } else {
            showToast(getString(R.string.generic_message_for_error))
        }
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(LoginViaEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewModel.loginViaStatus = it.loginViaStatus
                    when (it.loginViaStatus) {
                        LoginViaStatus.FACEBOOK -> {
                            showProgressBar()
                            facebookLogin()
                        }
                        LoginViaStatus.GMAIL -> {
                            showProgressBar()
                            gmailLogin()
                        }
                        LoginViaStatus.TRUECALLER -> {
                            showProgressBar()
                            trueCallerLogin()
                        }
                        LoginViaStatus.NUMBER_VERIFY -> {
                            viewModel.signUpAfterPhoneVerify(it.countryCode, it.mNumber)
                        }
                        else -> {
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
            this@SignUpActivity.finish()
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
                verificationThroughSinch(countryCode, phoneNumber, verificationVia)
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

    //Use link = https://developers.sinch.com/docs/verification-for-android
    private fun verificationThroughSinch(
        countryCode: String,
        phoneNumber: String,
        verificationVia: VerificationVia
    ) {
        if (phoneNumber.isEmpty()) {
            return
        }
        val listener = object : VerificationListener {
            override fun onInitiationFailed(e: Exception) {
                viewModel.verificationStatus.postValue(VerificationStatus.FAILED)
                e.printStackTrace()
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
        val defaultRegion: String? = PhoneNumberUtils.getDefaultCountryIso(this)
        AppAnalytics.create(AnalyticsEvent.SINCH_TEST.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.USER_PHONE_NUMBER.NAME, phoneNumber)
            .addParam(AnalyticsEvent.COUNTRY_FLAG_CHANGED.NAME, countryCode)
            .addParam(AnalyticsEvent.COUNTRY_ISO_CODE.NAME, defaultRegion ?: "NULL")
            .addParam(AnalyticsEvent.VERIFICATION_VIA_SINCH_TEST.NAME, verificationVia.toString())
            .push()
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
        viewModel.loginAnalyticsEvent(VerificationVia.SMS.name)
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
        viewModel.loginAnalyticsEvent(VerificationVia.FLASH_CALL.name)
        verification =
            SinchVerification.createFlashCallVerification(config, phoneNumberInE164, listener)
        verification?.initiate()
    }

    //Use link = https://docs.truecaller.com/truecaller-sdk/android/integrating-with-your-app/verifying-non-truecaller-users
    private fun verificationThroughTrueCaller(
        phoneNumber: String
    ) {
        val apiCallback: VerificationCallback = object : VerificationCallback {
            @SuppressLint("SwitchIntDef")
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
                                this@SignUpActivity,
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

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }

    override fun onDestroy() {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}

