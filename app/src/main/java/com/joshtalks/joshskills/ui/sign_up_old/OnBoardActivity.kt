package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.ErrorTag
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.signup.FROM_ACTIVITY
import com.joshtalks.joshskills.ui.signup.IS_ACTIVITY_FOR_RESULT
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.signup.SignUpViewModel
import com.truecaller.android.sdk.ITrueCallback
import com.truecaller.android.sdk.TrueError
import com.truecaller.android.sdk.TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER
import com.truecaller.android.sdk.TrueProfile
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils


class OnBoardActivity : CoreJoshActivity() {
    private lateinit var layout: ActivityOnboardBinding
    private var activityResultFlag = false
    private lateinit var appAnalytics: AppAnalytics


    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var prevScreen: String? = "Launcher"
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_onboard
        )
        layout.handler = this
        if (intent.hasExtra(IS_ACTIVITY_FOR_RESULT)) {
            activityResultFlag = intent?.getBooleanExtra(IS_ACTIVITY_FOR_RESULT, false) ?: false
        }
        if (intent.hasExtra("Flow")) {
            prevScreen = intent?.getStringExtra("Flow")
        }

        initTrueCallerSDK()
        appAnalytics = AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN_1.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, prevScreen)
        appAnalytics.push(true)
        val sBuilder = SpannableStringBuilder()
        sBuilder.append("Welcome to ").append("Josh Skills")
        val typefaceSpan =
            CalligraphyTypefaceSpan(TypefaceUtils.load(assets, "fonts/OpenSans-Bold.ttf"))
        sBuilder.setSpan(typefaceSpan, 11, 22, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        layout.textView.setText(sBuilder, TextView.BufferType.SPANNABLE)
        addObserver()
    }


    private fun addObserver() {
        viewModel.signUpStatus.observe(this, androidx.lifecycle.Observer {
            hideProgress()
            when (it) {
                SignUpStepStatus.SignUpCompleted -> {
                    if (activityResultFlag) {
                        setResult()
                        return@Observer
                    }
                    val intent = getIntentForState()
                    if (intent == null) {
                        AppAnalytics.create(AnalyticsEvent.LOGIN_SUCCESS.NAME).push()
                        startActivity(getInboxActivityIntent())
                    } else {
                        startActivity(intent)
                    }
                    finish()
                    return@Observer
                }
                SignUpStepStatus.SignUpWithoutRegister -> {
                    openCourseExplorerScreen(this@OnBoardActivity)
                    return@Observer
                }
                else -> return@Observer
            }
        })
        viewModel.progressDialogStatus.observe(this, androidx.lifecycle.Observer {
            if (it) {
                hideProgress()
            }
        })
    }

    fun signUp() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
            .push()
        val intent = Intent(this, SignUpActivity::class.java).apply {
            putExtra(IS_ACTIVITY_FOR_RESULT, activityResultFlag)
            putExtra(FROM_ACTIVITY, "onboarding journey")
        }
        startActivity(intent)
    }

    fun openCourseExplore() {
        AppAnalytics.create(AnalyticsEvent.EXPLORE_BTN_CLICKED.NAME)
            .addParam("name", this.javaClass.simpleName)
            .addBasicParam()
            .addUserDetails()
            .push()
        startActivity(Intent(applicationContext, CourseExploreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@OnBoardActivity.finish()
    }

    private fun initTrueCallerSDK() {
        /*try {
            val trueScope = TrueSdkScope.Builder(this, trueCallerSDKCallback)
                .consentMode(TrueSdkScope.CONSENT_MODE_POPUP)  //TrueSdkScope.CONSENT_MODE_POPUP
                .consentTitleOption(TrueSdkScope.SDK_CONSENT_TITLE_VERIFY)
                .footerType(TrueSdkScope.FOOTER_TYPE_CONTINUE) //TrueSdkScope.FOOTER_TYPE_CONTINUE
                .build()

            TrueSDK.init(trueScope)
            if (TrueSDK.getInstance().isUsable) {
                val locale = Locale("en")
                TrueSDK.getInstance().setLocale(locale)
                layout.loginTrueCallerContainer.visibility = View.VISIBLE
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }*/
    }

    fun verifyViaTrueCaller() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.TRUECALLER_PARAM.NAME)
            .push()
        // TrueSDK.getInstance().getUserProfile(this)
        showProgress()
        AppObjectController.uiHandler.postDelayed({ hideProgress() }, 1500)
    }

    private val trueCallerSDKCallback: ITrueCallback = object : ITrueCallback {
        override fun onSuccessProfileShared(@NonNull trueProfile: TrueProfile) {
            showProgress()
            viewModel.verifyUserViaTrueCaller(trueProfile)
        }

        override fun onVerificationRequired() {
        }

        override fun onFailureProfileShared(@NonNull trueError: TrueError) {
            if (trueError.errorType == ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER) {
                signUp()
            }
            LogException.catchError(ErrorTag.TRUE_CALLER, trueError.errorType.toString())
        }
    }

    private fun showProgress() {
        layout.btnTruecallerLogin.showProgress {
            buttonTextRes = R.string.login_with_truecaller_label
            progressColors = intArrayOf(
                ContextCompat.getColor(applicationContext, R.color.squash_light),
                Color.WHITE,
                ContextCompat.getColor(applicationContext, R.color.colorPrimary)
            )
            gravity = DrawableButton.GRAVITY_TEXT_END
            progressRadiusRes = R.dimen.dp4
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp2
        }
        layout.btnTruecallerLogin.isEnabled = false
    }

    private fun hideProgress() {
        try {
            layout.btnTruecallerLogin.isEnabled = true
            layout.btnTruecallerLogin.hideProgress(getString(R.string.login_with_truecaller_label))
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN_1.NAME).endSession()
    }

    override fun onResume() {
        super.onResume()
        if (Mentor.getInstance().hasId()) {
            startActivity(getInboxActivityIntent())
            finish()
        }
    }
}
