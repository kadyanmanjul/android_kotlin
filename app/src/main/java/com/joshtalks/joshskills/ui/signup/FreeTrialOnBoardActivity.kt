package com.joshtalks.joshskills.ui.signup

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_BODY_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_TITLE_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_YES_BUTTON_TEXT
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.IMPRESSION_OPEN_FREE_TRIAL_SCREEN
import com.joshtalks.joshskills.core.IMPRESSION_START_FREE_TRIAL
import com.joshtalks.joshskills.core.IMPRESSION_START_TRIAL_NO
import com.joshtalks.joshskills.core.IMPRESSION_START_TRIAL_YES
import com.joshtalks.joshskills.core.ONBOARDING_STAGE
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.OnBoardingStage
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.IMPRESSION_TRUECALLER_FREETRIAL_LOGIN
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.truecaller.android.sdk.*
import kotlinx.coroutines.CoroutineScope
import com.truecaller.android.sdk.TruecallerSDK
import com.truecaller.android.sdk.TruecallerSdkScope
import com.truecaller.android.sdk.ITrueCallback
import com.truecaller.android.sdk.TrueError
import com.truecaller.android.sdk.TrueProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_on_board
        )
        layout.handler = this
        layout.lifecycleOwner = this
        if (intent.getBooleanExtra(SHOW_SIGN_UP_FRAGMENT, false) &&
            Mentor.getInstance().getId().isNotEmpty()
        ) {
            openProfileDetailFragment()
        }
        addViewModelObservers()
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.APP_INSTALLED.value)
    }

    override fun onStart() {
        super.onStart()
        initTrueCallerUI()
        viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN)
    }

    override fun onPause() {
        super.onPause()
        hideProgressBar()
    }

    private fun addViewModelObservers() {
        viewModel.signUpStatus.observe(this, androidx.lifecycle.Observer {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.ProfileInCompleted -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        PrefManager.clearDatabase()
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
                        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
                    }
                    openProfileDetailFragment()
                }
                SignUpStepStatus.SignUpCompleted, SignUpStepStatus.ERROR -> {
                    openProfileDetailFragment()
                }
                else -> return@Observer
            }
        })
        viewModel.progressBarStatus.observe(this, androidx.lifecycle.Observer {
            showProgressBar()
        })
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

    fun showStartTrialPopup(language: ChooseLanguages) {
        viewModel.saveImpression(IMPRESSION_START_FREE_TRIAL)
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.START_NOW_CLICKED.value)
        PrefManager.put(FREE_TRIAL_TEST_ID, language.testId)
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
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FREE_TRIAL_POPUP_BODY_TEXT + language.testId)
                .replace("\\n", "\n")

        dialogView.findViewById<TextView>(R.id.add_a_topic).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FREE_TRIAL_POPUP_TITLE_TEXT + language.testId)

        dialogView.findViewById<TextView>(R.id.yes).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FREE_TRIAL_POPUP_YES_BUTTON_TEXT + language.testId)

        dialogView.findViewById<MaterialTextView>(R.id.yes).setOnClickListener {
            if (Mentor.getInstance().getId().isNotEmpty()) {
                viewModel.saveImpression(IMPRESSION_START_TRIAL_YES)
                PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.JI_HAAN_CLICKED.value)
                if (TruecallerSDK.getInstance().isUsable)
                    openTrueCallerBottomSheet()
                else
                    openProfileDetailFragment(language.testId)
                alertDialog.dismiss()
            }
        }

        dialogView.findViewById<MaterialTextView>(R.id.cancel).setOnClickListener {
            viewModel.saveImpression(IMPRESSION_START_TRIAL_NO)
            alertDialog.dismiss()
        }
    }

    fun initTrueCallerUI() {
        hideProgressBar()
        val trueScope = TruecallerSdkScope.Builder(this, sdkCallback)
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .ctaTextPrefix(TruecallerSdkScope.CTA_TEXT_PREFIX_CONTINUE_WITH)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().setLocale(Locale(PrefManager.getStringValue(USER_LOCALE)))
        }
    }

    private fun openTrueCallerBottomSheet() {
        showProgressBar()
        TruecallerSDK.getInstance().getUserProfile(this)
    }

    private val sdkCallback: ITrueCallback = object : ITrueCallback {
        override fun onFailureProfileShared(trueError: TrueError) {
            openProfileDetailFragment()
            hideProgressBar()
            if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                showToast(application.getString(R.string.internet_not_available_msz))
            }
        }

        override fun onVerificationRequired(p0: TrueError?) {
            openProfileDetailFragment()
        }

        override fun onSuccessProfileShared(trueProfile: TrueProfile) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_FREETRIAL_LOGIN)
                viewModel.userName = trueProfile.firstName
                viewModel.verifyUserViaTrueCaller(trueProfile)
                viewModel.isVerified = true
                openProfileDetailFragment()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance()
                .onActivityResultObtained(this, requestCode, resultCode, data)
            hideProgressBar()
            return
        }
        hideProgressBar()
    }

    private fun openProfileDetailFragment(testId: String = FREE_TRIAL_DEFAULT_TEST_ID) {
//        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(viewModel.userName ?: EMPTY, viewModel.isVerified).apply {
                    val bundle = Bundle()
                    bundle.putString(FREE_TRIAL_TEST_ID, testId)
                    arguments = bundle
                },
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

    fun openChooseLanguageFragment() {
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
}
