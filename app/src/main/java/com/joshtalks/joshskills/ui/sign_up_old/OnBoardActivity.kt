package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.crashlytics.android.Crashlytics
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.signup.IS_ACTIVITY_FOR_RESULT
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.signup.SignUpViewModel
import com.truecaller.android.sdk.*
import com.truecaller.android.sdk.TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils
import java.util.*


class OnBoardActivity : CoreJoshActivity() {
    private lateinit var layout: ActivityOnboardBinding
    private var activityResultFlag = false


    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_onboard
        )
        layout.handler = this
        if (intent.hasExtra(IS_ACTIVITY_FOR_RESULT)) {
            activityResultFlag = intent?.getBooleanExtra(IS_ACTIVITY_FOR_RESULT, false) ?: false
        }
        initTrueCallerSDK()
        AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN_1.NAME).push()
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
                    openCourseExplorerScreen()
                    return@Observer
                }
                SignUpStepStatus.CoursesNotExist -> {

                    if (activityResultFlag) {
                        setResult()
                        return@Observer
                    }
                    openCourseExplorerScreen()
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
        AppAnalytics.create(AnalyticsEvent.LOGIN_CLICKED.NAME).push()
        val intent = Intent(this, SignUpActivity::class.java).apply {
            putExtra(IS_ACTIVITY_FOR_RESULT, activityResultFlag)
        }
        startActivity(intent)
    }

    fun openCourseExplore() {
        startActivity(Intent(applicationContext, CourseExploreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
        this@OnBoardActivity.finish()

    }

    private fun initTrueCallerSDK() {
        try {
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
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun verifyViaTrueCaller() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_TRUECALLER_CLICKED.NAME).push()
        TrueSDK.getInstance().getUserProfile(this)
        showProgress()
        AppObjectController.uiHandler.postDelayed({ hideProgress() }, 1500)
    }


    private val trueCallerSDKCallback: ITrueCallback = object : ITrueCallback {
        override fun onSuccessProfileShared(@NonNull trueProfile: TrueProfile) {
            showProgress()
            viewModel.verifyUserViaTrueCaller(trueProfile)
            Log.e(TAG, "" + trueProfile.firstName)

        }

        override fun onVerificationRequired() {
            Crashlytics.log(3, "Truecaller Issue 2", "onVerificationRequired")
            Log.e(TAG, "onVerificationRequired")

        }

        override fun onFailureProfileShared(@NonNull trueError: TrueError) {
            if (trueError.errorType == ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER) {
                signUp()
            }
            Crashlytics.log(3, "Truecaller Issue", trueError.errorType.toString())
            Log.e(TAG, trueError.errorType.toString())

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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (TrueSDK.getInstance().isUsable) {
                TrueSDK.getInstance().onActivityResultObtained(this, resultCode, data)
            }
        } catch (ex: Exception) {
        }
    }

    private fun hideProgress() {
        try {
            layout.btnTruecallerLogin.isEnabled = true
            layout.btnTruecallerLogin.hideProgress(getString(R.string.login_with_truecaller_label))
        } catch (e: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (Mentor.getInstance().hasId()) {
            startActivity(getInboxActivityIntent())
            finish()
        }
    }
}
