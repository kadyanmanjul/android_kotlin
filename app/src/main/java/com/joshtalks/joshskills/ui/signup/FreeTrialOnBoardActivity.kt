package com.joshtalks.joshskills.ui.signup

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.truecaller.android.sdk.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }

    // truecaller login/signup viewmodel
    private val signUpViewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    // i don't know?
    private var compositeDisposable = CompositeDisposable()
    private lateinit var appAnalytics: AppAnalytics
    lateinit var name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        // added after successful running of app
        appAnalytics = AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(
                AnalyticsEvent.STATUS.NAME,
                AnalyticsEvent.FAILED_PARAM.NAME
            )
        super.onCreate(savedInstanceState)
        // added , may be of no use
        if (intent.hasExtra(FLOW_FROM))
            appAnalytics.addParam(
                AnalyticsEvent.FLOW_FROM_PARAM.NAME,
                intent.getStringExtra(FLOW_FROM)
            )

        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_on_board
        )
        layout.handler = this
        layout.lifecycleOwner = this
        /**
        if (intent.getBooleanExtra(SHOW_SIGN_UP_FRAGMENT, false) &&
        Mentor.getInstance().getId().isNotEmpty()
        ) {
        // openProfileDetailFragment()
        // openInboxActivity()
        }
         */


        // function for truecaller dialoge box initialising
        addViewModelObserver()
        setupTrueCaller()
        Toast.makeText(this, User.getInstance().isVerified.toString(), Toast.LENGTH_SHORT).show()
        if (User.getInstance().isVerified) {
            // openInboxActivity()
            moveToInboxScreen()
        }

        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.APP_INSTALLED.value)

    }


    private fun addViewModelObserver() {
        signUpViewModel.signUpStatus.observe(this, Observer {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.ProfileInCompleted -> {
                    //  binding.ivBack.visibility = View.GONE
                    lifecycleScope.launch(Dispatchers.IO) {
                        PrefManager.clearDatabase()
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
                    }
                    // openProfileDetailFragment(true)
                    submitProfile()
                }
                else -> return@Observer
            }
        })
        signUpViewModel.progressBarStatus.observe(this, Observer {
            showProgressBar()
        })
    }


    private fun addObservers() {

        signUpViewModel.signUpStatus.observe(this, androidx.lifecycle.Observer {
            when (it) {
                SignUpStepStatus.ProfileCompleted -> {
                    signUpViewModel.startFreeTrial(Mentor.getInstance().getId())
                    // must check
                    //  Toast.makeText(this@FreeTrialOnBoardActivity,
                    //   viewModel_true.startFreeTrial(Mentor.getInstance().getId()).toString() , Toast.LENGTH_SHORT).show()
                    Toast.makeText(
                        this@FreeTrialOnBoardActivity,
                        Mentor.getInstance().getUser()?.isVerified.toString() + " sakshi",
                        Toast.LENGTH_SHORT
                    ).show() // check this one
                    // moveToInboxScreen()
                }
                else -> {
                    //hideProgress()
                    return@Observer
                }
            }
        })
        signUpViewModel.apiStatus.observe(this, androidx.lifecycle.Observer {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    //  hideProgress()
                    //here necessary
                    moveToInboxScreen()
                    // logLoginSuccessAnalyticsEvent(viewModel_true.loginViaStatus?.toString())
                    // startActivity(getInboxActivityIntent())
                }
                else -> {
                    //  hideProgress()
                }
            }
        })
    }

    /**
    // newly added for login purpose
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
     */
    // added today , use
    fun submitProfile() {

        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = name
        requestMap["is_free_trial"] = "Y"
        signUpViewModel.completingProfile(requestMap, true) // false is right here
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.NAME_ENTERED.value)
    }

    // new added in this code
    private fun moveToInboxScreen() {

        AppAnalytics.create(AnalyticsEvent.FREE_TRIAL_ONBOARDING.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .push()

        val intent = Intent(this, InboxActivity::class.java).apply {
            putExtra(FLOW_FROM, "free trial onboarding journey")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }


    override fun onStart() {
        super.onStart()
        viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN)
    }

    /**
    private fun openInboxActivity() {
    startActivity(getInboxActivityIntent())
    this.finishAffinity()
    }
     */
    fun showStartTrialPopup() {
        viewModel.saveImpression(IMPRESSION_START_FREE_TRIAL)
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.START_NOW_CLICKED.value)
        layout.btnStartTrial.pauseAnimation()
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.freetrial_alert_dialog, null)
        dialogBuilder.setView(dialogView)

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
                // truecallersdk called
                if (packageManager.isAppInstalled("com.truecaller") && TruecallerSDK.getInstance().isUsable || Utils.isTrueCallerAppExist() && TruecallerSDK.getInstance().isUsable) {
                    Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show()
                    //   TruecallerSDK.getInstance().getUserProfile(this)

                    loginViaTrueCaller()
                    alertDialog.dismiss()
                } else {
                    openProfileDetailFragment()
                    alertDialog.dismiss()
                }
                // alertDialog.dismiss()
            }
        }

        dialogView.findViewById<MaterialTextView>(R.id.cancel).setOnClickListener {
            viewModel.saveImpression(IMPRESSION_START_TRIAL_NO)
            alertDialog.dismiss()
        }
    }

    // function for login viatruecaller
    fun loginViaTrueCaller() {
        signUpViewModel.loginAnalyticsEvent(LoginViaStatus.TRUECALLER.name)
        RxBus2.publish(LoginViaEventBus(LoginViaStatus.TRUECALLER))
    }

    // setup truecaller sdk scope
    private fun setupTrueCaller() {
        val trueScope = TruecallerSdkScope.Builder(this, object : ITrueCallback {
            override fun onFailureProfileShared(trueError: TrueError) {
                hideProgressBar()
                // Log.i(FreeTrialOnBoardActivity.TAG, trueError.getErrorType().toString() + "")
                if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                    showToast(application.getString(R.string.internet_not_available_msz))
                } else if (trueError.getErrorType() == TrueError.ERROR_PROFILE_NOT_FOUND) {
                    Toast.makeText(
                        this@FreeTrialOnBoardActivity,
                        "Incorrect Partner Key",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (trueError.getErrorType() == TrueError.ERROR_TYPE_UNAUTHORIZED_USER && trueError.getErrorType() == TrueError.ERROR_TYPE_INVALID_ACCOUNT_STATE) {
                    Toast.makeText(
                        this@FreeTrialOnBoardActivity,
                        "User not Verified on Truecaller*",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (trueError.getErrorType() == TrueError.ERROR_TYPE_TRUECALLER_CLOSED_UNEXPECTEDLY) {
                    Toast.makeText(
                        this@FreeTrialOnBoardActivity,
                        "Truecaller App Internal Error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onVerificationRequired(p0: TrueError?) {
                openProfileDetailFragment()
            }

            override fun onSuccessProfileShared(trueProfile: TrueProfile) {
                signUpViewModel.verifyUserViaTrueCaller(trueProfile)
               // name = trueProfile.firstName
                // submitProfile()
                //  logLoginSuccessAnalyticsEvent(viewModel_true.loginViaStatus?.toString())
                //  startActivity(getInboxActivityIntent())
                //  this@FreeTrialOnBoardActivity.finishAffinity()
                Toast.makeText(this@FreeTrialOnBoardActivity, "Registered", Toast.LENGTH_SHORT)
                    .show()
                //  submitProfile()
                //   var p : String = User.getInstance().isVerified.toString() + " after submit fun called"
                //   Toast.makeText(this@FreeTrialOnBoardActivity, p, Toast.LENGTH_SHORT).show()

            }

        })
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
           // val locale = Locale(PrefManager.getStringValue(USER_LOCALE))
            // add again
          //  val locale = Locale("hi")
          //  TruecallerSDK.getInstance().setLocale(locale)
        }
    }


    // onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val url = data?.data?.path ?: EMPTY
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().onActivityResultObtained(this, requestCode, resultCode, data)
            // return
        }
        hideProgressBar()

    }

    // for initiating all flows of truecaller
    private fun trueCallerLogin() {
        TruecallerSDK.getInstance().getUserProfile(this)
        // var locale = Locale("bn")
        // TruecallerSDK.getInstance().setLocale(locale)
        // Toast.makeText(this, User.getInstance().isVerified.toString(), Toast.LENGTH_SHORT).show()
        /**
        if (User.getInstance().isVerified) {
        openInboxActivity()
        }
         */
    }

    // resume and call observer
    override fun onResume() {
        super.onResume()
        addObserver()
        addObservers()
    }

    // observer
    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(LoginViaEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    signUpViewModel.loginViaStatus = it.loginViaStatus
                    when (it.loginViaStatus) {

                        LoginViaStatus.TRUECALLER -> {
                            //  showProgressBar()
                            trueCallerLogin()
                        }
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }


    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    // for checking truecaller is installed or not
    fun PackageManager.isAppInstalled(packageName: String): Boolean = try {
        getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        true
    } catch (e: Exception) {
        false
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

    // added, may be of no use
    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }

    // added, may be of no use
    override fun onDestroy() {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

}
