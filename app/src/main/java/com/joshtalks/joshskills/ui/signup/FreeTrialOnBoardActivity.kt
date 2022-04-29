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
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.IMPRESSION_OPEN_FREE_TRIAL_SCREEN
import com.joshtalks.joshskills.core.IMPRESSION_START_FREE_TRIAL
import com.joshtalks.joshskills.core.IMPRESSION_START_TRIAL_NO
import com.joshtalks.joshskills.core.IMPRESSION_START_TRIAL_YES
import com.joshtalks.joshskills.core.ONBOARDING_STAGE
import com.joshtalks.joshskills.core.IS_LOGIN_VIA_TRUECALLER
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.OnBoardingStage
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.FREE_TRIAL_TEST_ID
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_BODY_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_HUNDRED_POINTS_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_TITLE_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_POPUP_YES_BUTTON_TEXT
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.IMPRESSION_TRUECALLER_FREETRIAL_LOGIN
import com.joshtalks.joshskills.core.IMPRESSION_ALREADY_NEWUSER
import com.joshtalks.joshskills.core.IMPRESSION_TC_NOT_INSTALLED_JI_HAAN
import com.joshtalks.joshskills.core.IMPRESSION_TC_NOT_INSTALLED
import com.joshtalks.joshskills.core.IMPRESSION_TC_USER_ANOTHER
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.ui.activity_feed.utils.IS_USER_EXIST
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.truecaller.android.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"
const val HINDI_TO_ENGLISH_TEST_ID = "784"

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }
    private val liveEvent = EventLiveData

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
        liveEvent.observe(this) {
            when(it.what) {
                IS_USER_EXIST -> moveToInboxScreen()
            }
        }
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
        viewModel.progressBarStatus.observe(this, {
            showProgressBar()
        })
    }

    fun signUp() {
        MixPanelTracker.publishEvent(MixPanelEvent.LOGIN).push()
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
                .push()
            viewModel.saveTrueCallerImpression(IMPRESSION_ALREADY_NEWUSER)
            val intent = Intent(this@FreeTrialOnBoardActivity, SignUpActivity::class.java).apply {
                putExtra(FLOW_FROM, "free trial onboarding journey")
            }
            startActivity(intent)
        }
    }

    fun showStartTrialPopup(language: ChooseLanguages, is100PointsActive : Boolean) {
        MixPanelTracker.publishEvent(MixPanelEvent.START_NOW).push()
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

        if(is100PointsActive && language.testId == HINDI_TO_ENGLISH_TEST_ID){
            dialogView.findViewById<TextView>(R.id.e_g_motivat).text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FREE_TRIAL_POPUP_HUNDRED_POINTS_TEXT + language.testId)
                    .replace("\\n", "\n")
        }
        else {
            dialogView.findViewById<TextView>(R.id.e_g_motivat).text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FREE_TRIAL_POPUP_BODY_TEXT + language.testId)
                    .replace("\\n", "\n")
        }
        dialogView.findViewById<TextView>(R.id.add_a_topic).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FREE_TRIAL_POPUP_TITLE_TEXT + language.testId)

        dialogView.findViewById<TextView>(R.id.yes).text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FREE_TRIAL_POPUP_YES_BUTTON_TEXT + language.testId)

        dialogView.findViewById<MaterialTextView>(R.id.yes).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.JI_HAAN).push()
            if (Mentor.getInstance().getId().isNotEmpty()) {
                viewModel.saveImpression(IMPRESSION_START_TRIAL_YES)
                PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.JI_HAAN_CLICKED.value)
                if (TruecallerSDK.getInstance().isUsable)
                    openTrueCallerBottomSheet()
                else {
                    viewModel.saveTrueCallerImpression(IMPRESSION_TC_NOT_INSTALLED_JI_HAAN)
                    openProfileDetailFragment()
                }
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
        } else
            viewModel.saveTrueCallerImpression(IMPRESSION_TC_NOT_INSTALLED)
    }

    private fun openTrueCallerBottomSheet() {
        showProgressBar()
        TruecallerSDK.getInstance().getUserProfile(this)
    }

    private val sdkCallback: ITrueCallback = object : ITrueCallback {

        override fun onFailureProfileShared(trueError: TrueError) {
            if(TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER == trueError.errorType) {
                MixPanelTracker.publishEvent(MixPanelEvent.USE_ANOTHER_METHOD).push()
                hideProgressBar()
                viewModel.saveTrueCallerImpression(IMPRESSION_TC_USER_ANOTHER)
                openProfileDetailFragment()
            }

            if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                showToast(application.getString(R.string.internet_not_available_msz))
            }
        }

        override fun onVerificationRequired(p0: TrueError?) {
            openProfileDetailFragment()
        }

        override fun onSuccessProfileShared(trueProfile: TrueProfile) {
                PrefManager.put(IS_LOGIN_VIA_TRUECALLER,true)
                MixPanelTracker.publishEvent(MixPanelEvent.CONTINUE_WITH_NUMBER).push()
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_FREETRIAL_LOGIN)
                val user = User.getInstance()
                user.firstName = trueProfile.firstName
                user.phoneNumber = trueProfile.phoneNumber
                user.email = trueProfile.email
                user.gender = trueProfile.gender
                User.update(user)
                viewModel.userName = trueProfile.firstName
                viewModel.verifyUserViaTrueCaller(trueProfile)
                viewModel.isVerified = true
                openProfileDetailFragment()
        }
    }

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

    private fun openProfileDetailFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(viewModel.userName ?: EMPTY,
                    viewModel.isVerified),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
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

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
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
