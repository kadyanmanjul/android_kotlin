package com.joshtalks.joshskills.ui.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils.getLangCodeFromlangTestId
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.repository.server.onboarding.OnboardingCourseData
import com.joshtalks.joshskills.repository.server.onboarding.SpecificOnboardingCourseData
import com.joshtalks.joshskills.repository.server.signup.LastLoginType
import com.joshtalks.joshskills.ui.activity_feed.utils.IS_USER_EXIST
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.truecaller.android.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

const val SHOW_SIGN_UP_FRAGMENT = "SHOW_SIGN_UP_FRAGMENT"
const val HINDI_TO_ENGLISH_TEST_ID = "784"
const val ENGLISH_FOR_GOVERNMENT_EXAM_TEST_ID = "1906"
const val USER_CREATED_SUCCESSFULLY = 1002

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialOnBoardViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
    }
    private val liveEvent = EventLiveData
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
        layout.isLogin = PrefManager.getBoolValue(LOGIN_ONBOARDING, defValue = false)
        if (intent.getBooleanExtra(SHOW_SIGN_UP_FRAGMENT, false) &&
            Mentor.getInstance().getId().isNotEmpty()
        ) {
            openProfileDetailFragment()
        }
        initABTest()
        initOnboardingCourse()
        addViewModelObservers()
    }

    private fun initOnboardingCourse() {
        layout.onboardingData =
            if (PrefManager.hasKey(SPECIFIC_ONBOARDING, isConsistent = true)) {
                val courseId = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(SPECIFIC_ONBOARDING, isConsistent = true),
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
                USER_CREATED_SUCCESSFULLY -> openProfileDetailFragment()
            }
        }
        initTrueCallerUI()
        viewModel.saveImpression(IMPRESSION_OPEN_FREE_TRIAL_SCREEN)
    }

    override fun onPause() {
        super.onPause()
        hideProgressBar()
    }

    fun startTrial(v: View) {
        val language = ChooseLanguages(HINDI_TO_ENGLISH_TEST_ID, "Hindi (हिन्दी)")
        if (PrefManager.hasKey(SPECIFIC_ONBOARDING, isConsistent = true))
            signUp(v)
        else if (PrefManager.getStringValue(LAST_LOGIN_TYPE) == LastLoginType.VERIFIED_LOGIN.name)
            signUp(v)
        else if (PrefManager.getStringValue(LAST_LOGIN_TYPE) == LastLoginType.UNVERIFIED_LOGIN.name) {
            moveToInboxScreen()
            PrefManager.put(IS_GUEST_ENROLLED, true)
            PrefManager.put(IS_PAYMENT_DONE, false)
        } else if (PrefManager.hasKey(FT_COURSE_ONBOARDING)) {
            startFreeTrial(PrefManager.getStringValue(FT_COURSE_ONBOARDING))
        } else if (languageActive)
            openChooseLanguageFragment()
        else
            startFreeTrial(language.testId)
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
        viewModel.progressBarStatus.observe(this) {
            showProgressBar()
        }
    }

    fun signUp(v: View) {
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

    fun startFreeTrial(testId: String) {
        layout.btnStartTrial.pauseAnimation()
        PrefManager.put(FREE_TRIAL_TEST_ID, testId)
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
                openProfileDetailFragment()
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
            TruecallerSDK.getInstance().setLocale(Locale(PrefManager.getStringValue(USER_LOCALE)))
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
            PrefManager.put(IS_LOGIN_VIA_TRUECALLER, true)
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

    private fun openProfileDetailFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(null)
            replace(
                R.id.container,
                SignUpProfileForFreeTrialFragment.newInstance(
                    viewModel.userName ?: EMPTY,
                    viewModel.isVerified
                ),
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

    fun openGoalFragment() {
        viewModel.saveImpression(REASON_SCREEN_OPENED)
        supportFragmentManager.commit(true) {
            addToBackStack(ChooseGoalOnBoardFragment::class.java.name)
            replace(
                R.id.container,
                ChooseGoalOnBoardFragment.newInstance(),
                ChooseGoalOnBoardFragment::class.java.name
            )
        }
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
