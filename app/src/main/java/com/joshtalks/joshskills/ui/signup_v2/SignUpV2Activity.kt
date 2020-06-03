package com.joshtalks.joshskills.ui.signup_v2

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.truecaller.android.sdk.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

private const val GOOGLE_SIGN_UP_REQUEST_CODE = 9001

class SignUpV2Activity : BaseActivity() {

    private lateinit var appAnalytics: AppAnalytics
    private val viewModel: SignUpV2ViewModel by lazy {
        ViewModelProvider(this).get(SignUpV2ViewModel::class.java)
    }
    private lateinit var binding: ActivitySignUpV2Binding
    private var fbCallbackManager = CallbackManager.Factory.create()
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        appAnalytics = AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
        appAnalytics.push(true)
        super.onCreate(savedInstanceState)
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
    }

    private fun setupFacebookLogin() {
        LoginManager.getInstance().loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
        LoginManager.getInstance().registerCallback(fbCallbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    if (loginResult != null && loginResult.accessToken != null) {
                        getUserDetailsFromFB(loginResult.accessToken)
                    }
                }

                override fun onCancel() {
                }

                override fun onError(exception: FacebookException) {
                    exception.printStackTrace()
                }
            })
    }

    private fun setupTrueCaller() {
        val trueScope = TruecallerSdkScope.Builder(this, object : ITrueCallback {
            override fun onFailureProfileShared(p0: TrueError) {
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
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITH_OTP)
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
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpCompleteFragment.newInstance(),
                SignUpCompleteFragment::class.java.name
            )
        }
    }

    private fun openNumberVerificationFragment() {
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
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun gmailLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.GMAIL_PARAM.NAME)
            .push()

        val signInIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_UP_REQUEST_CODE)
    }

    private fun facebookLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.FACEBOOK_PARAM.NAME)
            .push()
        LoginManager.getInstance().logIn(this, listOf("public_profile", "email"))
    }

    private fun trueCallerLogin() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.TRUECALLER_PARAM.NAME)
            .push()
        TruecallerSDK.getInstance().getUserProfile(this@SignUpV2Activity)
    }

    fun getUserDetailsFromFB(accessToken: AccessToken) {
        val request: GraphRequest = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            val id = jsonObject.getString("id")
            var name: String? = null
            if (jsonObject.has("name")) {
                name = jsonObject.getString("name")
            }
            var email: String? = null
            if (jsonObject.has("email")) {
                email = jsonObject.getString("name")
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
            this@SignUpV2Activity.finish()
            return
        }
    }

}

