package com.joshtalks.joshskills.ui.signup

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.truecaller.android.sdk.*
import com.truecaller.android.sdk.clients.VerificationCallback
import com.truecaller.android.sdk.clients.VerificationDataBundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

const val GOOGLE_SIGN_UP_REQUEST_CODE = 9001
const val FLOW_FROM = "Flow"

class SignUpActivity : ThemedBaseActivity() {

    private lateinit var appAnalytics: AppAnalytics
    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this)[SignUpViewModel::class.java]
    }
    private val viewModelForDpUpload: UserProfileViewModel by lazy {
        ViewModelProvider(this)[UserProfileViewModel::class.java]
    }
    private lateinit var binding: ActivitySignUpV2Binding
    private var fbCallbackManager = CallbackManager.Factory.create()
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var compositeDisposable = CompositeDisposable()

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("signup", "onCreate: ")
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
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel
        viewModel.shouldStartFreeTrial = intent.getBooleanExtra(START_FREE_TRIAL, false)
        addViewModelObserver()
        initLoginFeatures()
        setupTrueCaller()
        if (PrefManager.hasKey(FT_ONBOARDING_NEXT_STEP)) {
            SignUpStepStatus.valueOf(PrefManager.getStringValue(FT_ONBOARDING_NEXT_STEP)).let {
                viewModel.updateFTSignUpStatus(it)
            }
        } else if (viewModel.shouldStartFreeTrial.not()) {
            if (User.getInstance().isVerified && isUserProfileComplete()) {
                openProfileDetailFragment(false)
            } else if (User.getInstance().isVerified && !isRegProfileComplete()) {
                openProfileDetailFragment(true)
            } else
                openSignUpOptionsFragment()
        } else {
            if (isVariantActive(VariantKeys.NEW_LOGIN_BEFORE_NAME)) {
                openSignUpOptionsFragment()
            } else if (isVariantActive(VariantKeys.NEW_LANGUAGE_ENABLED)) {
                openChooseLanguageFragment()
            } else {
                openSignUpOptionsFragment()
            }
        }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun addViewModelObserver() {
        var isFirstTime = false
        viewModel.signUpStatus.observe(this, Observer {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.RequestForOTP -> {
                    openNumberVerificationFragment()
                }
                SignUpStepStatus.LanguageSelection -> {
                    openChooseLanguageFragment()
                }
                SignUpStepStatus.ProfileInCompleted -> {
                    binding.ivBack.visibility = View.GONE
                    lifecycleScope.launch(Dispatchers.IO) {
                        PrefManager.clearDatabase()
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
                    }
                    isFirstTime = true
                    viewModel.saveTrueCallerImpression(IMPRESSION_ALREADY_NEWUSER_ENROLL)
                    openProfileDetailFragment(true)
                }
                SignUpStepStatus.NameSubmitted -> {
                    openSignUpOptionsFragment()
                }
                SignUpStepStatus.StartTrial -> {
                    viewModel.startFreeTrial(Mentor.getInstance().getId())
                }
                SignUpStepStatus.ProfileCompleted -> {
                    binding.ivBack.visibility = View.GONE
                    openProfilePicUpdateFragment()
                }
                SignUpStepStatus.ProfilePicUploaded -> {
                    binding.ivBack.visibility = View.GONE
                    binding.skip.visibility = View.INVISIBLE
                    openProfilePicSuccessfullyUpdateFragment()
                }
                SignUpStepStatus.StartAfterPicUploaded, SignUpStepStatus.ProfilePicSkipped, SignUpStepStatus.SignUpCompleted -> {
                    logLoginSuccessAnalyticsEvent(viewModel.loginViaStatus?.toString())
                    if (!isFirstTime)
                        viewModel.saveTrueCallerImpression(IMPRESSION_ALREADY_ALREADYUSER)
                    if (PrefManager.hasKey(SPECIFIC_ONBOARDING, isConsistent = true))
                        viewModel.registerSpecificCourse()
                    else {
                        startActivity(getInboxActivityIntent())
                        this@SignUpActivity.finishAffinity()
                    }
                }
                else -> return@Observer
            }
        })
        viewModel.progressBarStatus.observe(this, Observer {
            showProgressBar()
        })

        viewModel.fromVerificationScreen.observe(this, Observer {
            if (it)
                addRetryCountAnalytics()
        })
        viewModel.apiStatus.observe(this) {
            when (it) {
                ApiCallStatus.START -> showProgressBar()
                ApiCallStatus.SUCCESS -> {
                    startActivity(getInboxActivityIntent())
                    this@SignUpActivity.finishAffinity()
                }
                ApiCallStatus.FAILED -> {
                    hideProgressBar()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.internet_not_available_msz),
                        Snackbar.LENGTH_SHORT
                    ).setAction(getString(R.string.retry)) {
                        viewModel.registerSpecificCourse()
                    }.show()
                }
                else -> {}

            }
        }
        viewModel.freeTrialEntity.observe(this) {
            if (it != null) {
                hideProgressBar()
                moveToConversationScreen(it)
            }
        }
        viewModelForDpUpload.apiCallStatus.observe(this, Observer
        {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    hideProgressBar()
                    viewModel.changeSignupStatusToProfilePicUploaded()
                }
                ApiCallStatus.FAILED -> {
                    hideProgressBar()
                }
                ApiCallStatus.START -> {
                    showProgressBar()
                }
                else -> {

                }
            }
        })
    }

    private fun openChooseLanguageFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                ChooseLanguageOnBoardFragment.newInstance(),
                ChooseLanguageOnBoardFragment::class.java.name
            )
        }
    }

    fun openChooseGoalFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(ChooseGoalOnBoardFragment::class.java.name)
            replace(
                R.id.container,
                ChooseGoalOnBoardFragment.newInstance(),
                ChooseGoalOnBoardFragment::class.java.name
            )
        }
    }

    fun onLanguageSelected(testId: String) {
        PrefManager.put(FREE_TRIAL_TEST_ID, testId)
        viewModel.postGoal(GoalKeys.LANGUAGE_SELECTED)
        if (testId == HINDI_TO_ENGLISH_TEST_ID && isVariantActive(VariantKeys.ENGLISH_FOR_GOVT_EXAM_ENABLED)) {
            openChooseGoalFragment()
        } else {
            openSignUpNameFragment()
        }
    }

    fun onGoalSelected(testId: String) {
        PrefManager.put(FREE_TRIAL_TEST_ID, testId)
        viewModel.postGoal(GoalKeys.REASON_SELECTED)
        openSignUpNameFragment()
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
    }

    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().requestId().requestProfile().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }


    private fun setupTrueCaller() {
        val trueScope = TruecallerSdkScope.Builder(this, object : ITrueCallback {
            override fun onFailureProfileShared(trueError: TrueError) {
                hideProgressBar()
                if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                    showToast(application.getString(R.string.internet_not_available_msz))
                }
                if (TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER == trueError.errorType) {
                    MixPanelTracker.publishEvent(MixPanelEvent.TRUECALLER_VERIFICATION_SKIP).push()
                } else {
                    MixPanelTracker.publishEvent(MixPanelEvent.TRUECALLER_VERIFICATION_CONTD)
                        .addParam(ParamKeys.IS_SUCCESS, false)
                        .push()
                }
            }

            override fun onVerificationRequired(p0: TrueError?) {

            }

            override fun onSuccessProfileShared(trueProfile: TrueProfile) {
                viewModel.postGoal(GoalKeys.TRUECALLER_SELECTED)
                viewModel.verifyUserViaTrueCaller(trueProfile)
                MixPanelTracker.publishEvent(MixPanelEvent.TRUECALLER_VERIFICATION_CONTD)
                    .addParam(ParamKeys.IS_SUCCESS, true)
                    .push()
            }

        })
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .ctaTextPrefix(TruecallerSdkScope.CTA_TEXT_PREFIX_CONTINUE_WITH)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
            val locale = Locale(PrefManager.getStringValue(USER_LOCALE))
            TruecallerSDK.getInstance().setLocale(locale)
        } else {
            viewModel.saveTrueCallerImpression(IMPRESSION_TC_NOT_INSTALLED)
        }
    }

    private fun openSignUpOptionsFragment() {
        binding.skip.visibility = View.GONE
        binding.ivHelp.visibility = View.GONE
//        binding.ivPrivacy.visibility = View.VISIBLE
        if (TruecallerSDK.getInstance().isUsable)
            trueCallerLogin()
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpOptionsFragment::class.java.name)
            replace(
                R.id.container,
                SignUpOptionsFragment.newInstance(),
                SignUpOptionsFragment::class.java.name
            )
        }
    }

    private fun openProfileDetailFragment(isRegistrationScreenFirstTime: Boolean) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileFragment.newInstance(isRegistrationScreenFirstTime),
                SignUpProfileFragment::class.java.name
            )
        }
    }

    private fun openSignUpNameFragment() {
        val testId = PrefManager.getStringValue(FREE_TRIAL_TEST_ID)
        if (testId == HINDI_TO_ENGLISH_TEST_ID || testId == ENGLISH_FOR_GOVERNMENT_EXAM_TEST_ID)
            requestWorkerForChangeLanguage("en", canCreateActivity = false)
        else
            requestWorkerForChangeLanguage(Utils.getLangCodeFromlangTestId(testId), canCreateActivity = false)
        if (isVariantActive(VariantKeys.ORIGINAL_LOGIN_FLOW) && TruecallerSDK.getInstance().isUsable) {
            trueCallerLogin()
        }
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment(),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
        }
    }

    private fun openProfilePicUpdateFragment() {
        binding.skip.visibility = View.VISIBLE
        binding.ivHelp.visibility = View.GONE
//        binding.ivPrivacy.visibility = View.GONE
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfilePicUpdateFragment.newInstance(),
                SignUpProfilePicUpdateFragment.TAG
            )
        }
    }

    private fun openProfilePicSuccessfullyUpdateFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfilePicSuccessfullyUpdatedFragment.newInstance(),
                SignUpProfilePicSuccessfullyUpdatedFragment.TAG
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
        val url = data?.data?.path ?: EMPTY
        if (url.isNotBlank() && resultCode == Activity.RESULT_OK) {
            val imageUpdatedPath = AppDirectory.getImageSentFilePath()
            AppDirectory.copy(url, imageUpdatedPath)
            viewModelForDpUpload.uploadMedia(imageUpdatedPath)
        } else if (requestCode == GOOGLE_SIGN_UP_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                hideProgressBar()
                MixPanelTracker.publishEvent(MixPanelEvent.GOOGLE_VERIFICATION)
                    .addParam(ParamKeys.IS_SUCCESS, false)
                    .push()
                if (BuildConfig.DEBUG) {
                    showToast(getString(R.string.gmail_login_error_message))
                }
            } catch (e: Exception) {
                MixPanelTracker.publishEvent(MixPanelEvent.GOOGLE_VERIFICATION)
                    .addParam(ParamKeys.IS_SUCCESS, false)
                    .push()
                hideProgressBar()
                LogException.catchException(e)
            }
            return
        }
        fbCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().onActivityResultObtained(this, requestCode, resultCode, data)
            return
        }
        hideProgressBar()

    }

    private fun gmailLogin() {
        val signInIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_UP_REQUEST_CODE)
    }

    private fun facebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
    }

    private fun trueCallerLogin() {
        TruecallerSDK.getInstance().getUserProfile(this@SignUpActivity)
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        showWebViewDialog(url)
    }

    fun onSkipPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.SKIP_CLICKED).push()
        logSkipEvent()
        viewModel.changeSignupStatusToProfilePicSkipped()
    }

    private fun logSkipEvent() {
        AppAnalytics.create(AnalyticsEvent.SKIP_PROFILE_PIC.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        if (account.idToken.isNullOrEmpty().not()) {
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val accountUser = auth.currentUser
                        handleFirebaseAuth(accountUser)
                        MixPanelTracker.publishEvent(MixPanelEvent.GOOGLE_VERIFICATION)
                            .addParam(ParamKeys.IS_SUCCESS, true)
                            .push()
                    } else {
                        task.exception?.showAppropriateMsg()

                        showToast(getString(R.string.generic_message_for_error))
                        MixPanelTracker.publishEvent(MixPanelEvent.GOOGLE_VERIFICATION)
                            .addParam(ParamKeys.IS_SUCCESS, false)
                            .push()
                    }
                }
        } else {
            MixPanelTracker.publishEvent(MixPanelEvent.GOOGLE_VERIFICATION)
                .addParam(ParamKeys.IS_SUCCESS, false)
                .push()
            showToast(getString(R.string.generic_message_for_error))
        }
    }

    private fun handleFirebaseAuth(accountUser: FirebaseUser?) {
        if (accountUser != null) {
            viewModel.signUpUsingSocial(
                LoginViaStatus.GMAIL,
                accountUser.uid,
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
                        LoginViaStatus.GMAIL -> {
                            showProgressBar()
                            gmailLogin()
                        }
                        LoginViaStatus.TRUECALLER -> {
                            showProgressBar()
                            if (TruecallerSDK.getInstance().isUsable)
                                trueCallerLogin()
                            else
                                showToast(getString(R.string.something_went_wrong))
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

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        supportFragmentManager.popBackStackImmediate()
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (PrefManager.getStringValue(USER_LOCALE) != "en")
                requestWorkerForChangeLanguage("en", canCreateActivity = false)
            this@SignUpActivity.finish()
            return
        }
    }

    fun createVerification(
        countryCode: String,
        phoneNumber: String,
        service: VerificationService = VerificationService.SMS_COUNTRY,
        verificationVia: VerificationVia = VerificationVia.SMS
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

    fun moveToConversationScreen(inboxEntity: InboxEntity) {
        PrefManager.put(CURRENT_COURSE_ID, inboxEntity.courseId)
        PendingIntent.getActivities(
            this,
            (System.currentTimeMillis() and 0xfffffff).toInt(),
            arrayOf(
                Intent(this, InboxActivity::class.java).apply {
                    putExtra(FLOW_FROM, "free trial onboarding journey")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                ConversationActivity.getConversationActivityIntent(this, inboxEntity)
            ),
            PendingIntent.FLAG_UPDATE_CURRENT
        ).send()
    }

    private fun isVariantActive(variantKey: VariantKeys) = viewModel.abTestRepository.isVariantActive(variantKey)

    companion object {
        private const val START_FREE_TRIAL = "start_free_trial"

        @JvmStatic
        fun start(context: Context, flowFrom: String, shouldStartFreeTrial: Boolean = false) {
            val starter = Intent(context, SignUpActivity::class.java)
                .putExtra(FLOW_FROM, flowFrom)
                .putExtra(START_FREE_TRIAL, shouldStartFreeTrial)
            context.startActivity(starter)
        }
    }


}

