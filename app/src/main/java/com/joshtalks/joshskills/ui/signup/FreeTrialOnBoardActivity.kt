package com.joshtalks.joshskills.ui.signup

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.signup.SignUpProfileFragment.Companion.newInstance
import com.joshtalks.joshskills.ui.signup.SignUpProfilePicSuccessfullyUpdatedFragment.Companion.newInstance
import com.joshtalks.joshskills.ui.signup.SignUpProfilePicUpdateFragment.Companion.newInstance
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.truecaller.android.sdk.TruecallerSDK

import com.truecaller.android.sdk.TruecallerSdkScope
import com.truecaller.android.sdk.TrueError

import com.truecaller.android.sdk.TrueProfile

import com.truecaller.android.sdk.ITrueCallback
import kotlinx.coroutines.CoroutineScope
import java.util.*


const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }
    private lateinit var binding: ActivityFreeTrialOnBoardBinding
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
//        if (intent.getBooleanExtra(SHOW_SIGN_UP_FRAGMENT, false) &&
//            Mentor.getInstance().getId().isNotEmpty()
//        ) {
//            openProfileDetailFragment()
//        }
//        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.APP_INSTALLED.value)
        initTrueCallerUI()
        //if truecaller is there in your phone then the FreetrialOnboardActivity will trigger and the tc popup will open in this actvity
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
    fun tc() {
//        showProgressBar()
//        if (TruecallerSDK.getInstance().isUsable()) {
//            TruecallerSDK.getInstance().getUserProfile(this@FreeTrialOnBoardActivity)
//        } else { //else open the ji haan pop up
//            showStartTrialPopup()
//        }
        TruecallerSDK.getInstance().getUserProfile(this@FreeTrialOnBoardActivity)
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

//    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == TruecallerSDK.SHARE_PROFILE_REQUEST_CODE) {
//            TruecallerSDK.getInstance()
//                .onActivityResultObtained(this, requestCode, resultCode, data)
//        }
//        if (TruecallerSDK.getInstance().isUsable) {
//            TruecallerSDK.getInstance().onActivityResultObtained(this, requestCode, resultCode, data)
//            return
//        }
//        hideProgressBar()
//    }

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
        viewModel.signUpStatus.observe(this,{
            hideProgressBar()
            when (it) {
//                SignUpStepStatus.RequestForOTP -> {
//                    openNumberVerificationFragment()
//                }
//                SignUpStepStatus.ProfileInCompleted -> {
//                    binding.ivBack.visibility = View.GONE
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        PrefManager.clearDatabase()
//                        PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
//                        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
//                    }
//                    openProfileDetailFragment(true)
//                }
                SignUpStepStatus.ProfileCompleted -> {
                    Log.e("Ayaaz", "addViewModelObserver: ")
//                    binding.ivBack.visibility = View.GONE
//                    openProfilePicUpdateFragment()
                }
//                SignUpStepStatus.ProfilePicUploaded -> {
//                    binding.ivBack.visibility = View.GONE
//                    binding.skip.visibility = View.INVISIBLE
//                    openProfilePicSuccessfullyUpdateFragment()
//                }
                SignUpStepStatus.SignUpCompleted -> {
                    Log.e("Ayaaz", "addViewModelObserver: ")
                    startActivity(getSignUpProfileForFreeTrialFragmentIntent())
                    this@FreeTrialOnBoardActivity.finishAffinity()
                }
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        TruecallerSDK.clear()
    }

}