package com.joshtalks.joshskills.premium.ui.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.constants.IS_USER_EXIST
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.Utils.getLangCodeFromlangTestId
import com.joshtalks.joshskills.premium.core.abTest.GoalKeys
import com.joshtalks.joshskills.premium.core.abTest.VariantKeys
import com.joshtalks.joshskills.premium.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.repository.server.ChooseLanguages
import com.joshtalks.joshskills.premium.repository.server.onboarding.OnboardingCourseData
import com.joshtalks.joshskills.premium.repository.server.onboarding.SpecificOnboardingCourseData
import com.joshtalks.joshskills.premium.repository.server.signup.LastLoginType
import com.joshtalks.joshskills.premium.ui.inbox.InboxActivity
import com.truecaller.android.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

const val HINDI_TO_ENGLISH_TEST_ID = "784"
const val ENGLISH_FOR_GOVERNMENT_EXAM_TEST_ID = "1906"
const val USER_CREATED_SUCCESSFULLY = 1002

class FreeTrialOnBoardActivity : ThemedCoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this)[FreeTrialOnBoardViewModel::class.java]
    }
    private var languageActive = false
    private var eftActive = false
    private var is100PointsActive = false
    private var increaseCoursePrice = false
    val jsonData = JSONObject()
    val parameters = HashMap<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_on_board
        )
        layout.handler = this
        layout.lifecycleOwner = this
        viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN)
        viewModel.logImpressionFromWorker()
        initABTest()
        initOnboardingCourse()
        addViewModelObservers()
        com.joshtalks.joshskills.premium.core.PrefManager.getBoolValue(
            com.joshtalks.joshskills.premium.core.LOGIN_ONBOARDING, defValue = false).let { isLogin ->
            layout.btnStartTrialText.apply {
                text = if (isLogin) "Sign In" else
                    "Start Now"
                setOnClickListener {
                    viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN_CLICK)
                    if (isLogin)
                        signUp(it)
                    else {
                        viewModel.postGoal(GoalKeys.START_NOW_BUTTON_CLICKED)
                        viewModel.saveImpression(IMPRESSION_START_FREE_TRIAL)
                        startTrial(it)
                    }
                }
            }
            layout.txtLogin.setOnClickListener {
                viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN_CLICK)
                if (isLogin) startTrial(it)
                else signUp(it)
            }
        }
    }

    private fun initOnboardingCourse() {
        layout.onboardingData =
            if (com.joshtalks.joshskills.premium.core.PrefManager.hasKey(
                    com.joshtalks.joshskills.premium.core.SPECIFIC_ONBOARDING, isConsistent = true)) {
                val courseId = AppObjectController.gsonMapper.fromJson(
                    com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                        com.joshtalks.joshskills.premium.core.SPECIFIC_ONBOARDING, isConsistent = true),
                    SpecificOnboardingCourseData::class.java
                )?.courseId
                AppObjectController.gsonMapper.fromJson(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString("ONBOARDING_COURSE_$courseId"),
                    OnboardingCourseData::class.java
                )
            } else {
                OnboardingCourseData(
                    getString(R.string.onboarding_course_heading),
                    getString(R.string.onboarding_course_info1),
                    getString(R.string.onboarding_course_info2),
                    getString(R.string.onboarding_course_info3),
                )
            }
    }

    override fun onStart() {
        super.onStart()
        viewModel.liveEvent.observe(this) {
            when (it.what) {
                IS_USER_EXIST -> moveToInboxScreen()
                USER_CREATED_SUCCESSFULLY -> openSignUpNameFragment()
            }
        }
        initTrueCallerUI()
    }

    override fun onPause() {
        super.onPause()
        hideProgressBar()
    }

    fun startTrial(v: View) {
        val language = ChooseLanguages(HINDI_TO_ENGLISH_TEST_ID, "Hindi (हिन्दी)")
        if (com.joshtalks.joshskills.premium.core.PrefManager.hasKey(
                com.joshtalks.joshskills.premium.core.SPECIFIC_ONBOARDING, isConsistent = true))
            signUp(v)
        else if (com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                com.joshtalks.joshskills.premium.core.LAST_LOGIN_TYPE
            ) == LastLoginType.VERIFIED_LOGIN.name)
            signUp(v)
        else if (com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                com.joshtalks.joshskills.premium.core.LAST_LOGIN_TYPE
            ) == LastLoginType.UNVERIFIED_LOGIN.name) {
            moveToInboxScreen()
            com.joshtalks.joshskills.premium.core.PrefManager.put(
                com.joshtalks.joshskills.premium.core.IS_GUEST_ENROLLED, true)
            com.joshtalks.joshskills.premium.core.PrefManager.put(
                com.joshtalks.joshskills.premium.core.IS_PAYMENT_DONE, false)
        } else if (com.joshtalks.joshskills.premium.core.PrefManager.hasKey(
                com.joshtalks.joshskills.premium.core.FT_COURSE_ONBOARDING
            )) {
            startFreeTrial(
                com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                    com.joshtalks.joshskills.premium.core.FT_COURSE_ONBOARDING
                ))
        } else if (languageActive) {
            signUp(v, shouldStartFreeTrial = true)
        } else {
            startFreeTrial(language.testId)
        }
    }


    private fun addViewModelObservers() {
        viewModel.signUpStatus.observe(this) {
            hideProgressBar()
            when (it) {
                SignUpStepStatus.ProfileInCompleted -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        com.joshtalks.joshskills.premium.core.PrefManager.clearDatabase()
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.ONLINE_TEST_LAST_LESSON_COMPLETED, 0)
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED, 0)
                    }
                    openSignUpNameFragment()
                }
                SignUpStepStatus.SignUpCompleted, SignUpStepStatus.ERROR -> {
                    openSignUpNameFragment()
                }
                else -> return@observe
            }
        }
        viewModel.progressBarStatus.observe(this) {
            showProgressBar()
        }
    }

    fun signUp(v: View, shouldStartFreeTrial: Boolean = false) {
        MixPanelTracker.publishEvent(MixPanelEvent.LOGIN).push()
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
                .push()
            viewModel.saveTrueCallerImpression(IMPRESSION_ALREADY_NEWUSER)
            SignUpActivity.start(
                this@FreeTrialOnBoardActivity,
                "free trial onboarding journey",
                shouldStartFreeTrial
            )
        }
    }

    fun startFreeTrial(testId: String) {
        layout.btnStartTrial.pauseAnimation()
        com.joshtalks.joshskills.premium.core.PrefManager.put(FREE_TRIAL_TEST_ID, testId)
        if (testId == HINDI_TO_ENGLISH_TEST_ID || testId == ENGLISH_FOR_GOVERNMENT_EXAM_TEST_ID) {
            requestWorkerForChangeLanguage("en", canCreateActivity = false)
        } else {
            requestWorkerForChangeLanguage(getLangCodeFromlangTestId(testId), canCreateActivity = false)
        }
        if (Mentor.getInstance().getId().isNotEmpty()) {
            if (TruecallerSDK.getInstance().isUsable)
                openTrueCallerBottomSheet()
            else {
                viewModel.saveTrueCallerImpression(IMPRESSION_TC_NOT_INSTALLED_JI_HAAN)
                openSignUpNameFragment()
            }
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
            TruecallerSDK.getInstance().setLocale(Locale(
                com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                    com.joshtalks.joshskills.premium.core.USER_LOCALE
                )))
        } else
            viewModel.saveTrueCallerImpression(IMPRESSION_TC_NOT_INSTALLED)
    }

    private fun openTrueCallerBottomSheet() {
        showProgressBar()
        viewModel.saveTrueCallerImpression(TC_BOTTOMSHEET_SHOWED)
        TruecallerSDK.getInstance().getUserProfile(this)
    }

    private val sdkCallback: ITrueCallback = object : ITrueCallback {

        override fun onFailureProfileShared(trueError: TrueError) {
            if (TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER == trueError.errorType) {
                MixPanelTracker.publishEvent(MixPanelEvent.USE_ANOTHER_METHOD).push()
                hideProgressBar()
                viewModel.saveTrueCallerImpression(IMPRESSION_TC_USER_ANOTHER)
                openSignUpNameFragment()
            }

            if (TrueError.ERROR_TYPE_NETWORK == trueError.errorType) {
                showToast(application.getString(R.string.internet_not_available_msz))
            }
        }

        override fun onVerificationRequired(p0: TrueError?) {
            openSignUpNameFragment()
        }

        override fun onSuccessProfileShared(trueProfile: TrueProfile) {
            com.joshtalks.joshskills.premium.core.PrefManager.put(
                com.joshtalks.joshskills.premium.core.IS_LOGIN_VIA_TRUECALLER, true)
            MixPanelTracker.publishEvent(MixPanelEvent.CONTINUE_WITH_NUMBER).push()
            viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_FREETRIAL_LOGIN)
            val user = User.getInstance()
            user.firstName = trueProfile.firstName
            user.phoneNumber = trueProfile.phoneNumber
            user.email = trueProfile.email
            user.gender = trueProfile.gender
            user.isVerified = true
            User.update(user)
            viewModel.userName = trueProfile.firstName
            viewModel.isVerified = true
            viewModel.verifyUserViaTrueCaller(trueProfile)
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

    private fun openSignUpNameFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment(),
                SignUpProfileForFreeTrialFragment::class.java.name
            )
        }
    }

    fun loginText(): Spannable {
        val isLoggedIn = com.joshtalks.joshskills.premium.core.PrefManager.getBoolValue(
            com.joshtalks.joshskills.premium.core.LOGIN_ONBOARDING, defValue = false)
        return if (isLoggedIn){
            val text = getString(R.string.not_a_user_sign_up)
            val spannable = SpannableString(text)
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_500)), 12, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable
        } else {
            val text = getString(R.string.already_a_user_login)
            val spannable = SpannableString(text)
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_500)), 16, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable
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
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        showWebViewDialog(url)
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
//            this@FreeTrialOnBoardActivity.finish()
            return
        }
        super.onBackPressed()
    }

    fun initABTest() {
        ABTestRepository().apply {
            languageActive = isVariantActive(VariantKeys.NEW_LANGUAGE_ENABLED)
            eftActive = isVariantActive(VariantKeys.EFT_ENABLED)
            increaseCoursePrice = isVariantActive(VariantKeys.ICP_ENABLED)
            is100PointsActive = isVariantActive(VariantKeys.POINTS_HUNDRED_ENABLED)
        }
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, FreeTrialOnBoardActivity::class.java)
    }
}
