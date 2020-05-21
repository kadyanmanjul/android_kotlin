package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.Gravity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.ChangeBounds
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivitySignUpBinding

const val IS_ACTIVITY_FOR_RESULT = "is_activity_for_result"
const val FROM_ACTIVITY = "from_activity"

class SignUpActivity : CoreJoshActivity() {

    private lateinit var layout: ActivitySignUpBinding
    private var activityResultFlag = false
    private var fromActivity = EMPTY
    private lateinit var appAnalytics :AppAnalytics
    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layout = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        layout.handler = this
        supportActionBar?.hide()
        if (intent.hasExtra(IS_ACTIVITY_FOR_RESULT)) {
            activityResultFlag = intent?.getBooleanExtra(IS_ACTIVITY_FOR_RESULT, false) ?: false
            fromActivity = intent?.getStringExtra(FROM_ACTIVITY) ?: EMPTY
        }
        addObserver()
        login()
        appAnalytics= AppAnalytics.create(AnalyticsEvent.SIGNUP_SATUS.NAME)
            .addBasicParam()
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
            .addUserDetails()
        if(fromActivity.isEmpty().not())
            appAnalytics.addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME,fromActivity)
    }

    private fun login() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpStep1Fragment::class.java.name)
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            add(
                R.id.container,
                SignUpStep1Fragment.newInstance(),
                SignUpStep1Fragment::class.java.name
            )
        }
    }

    private fun addObserver() {
        viewModel.signUpStatus.observe(this, Observer {
            when (it) {
                SignUpStepStatus.SignUpStepFirst -> {
                    supportFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    login()
                    return@Observer
                }
                SignUpStepStatus.SignUpStepSecond -> {
                    addNextFragment()
                    return@Observer
                }
                SignUpStepStatus.SignUpCompleted -> {
                    appAnalytics.addParam(AnalyticsEvent.STATUS.NAME,AnalyticsEvent.SUCCESS_PARAM.NAME)
                    if(viewModel.phoneNumber.isEmpty().not())
                    appAnalytics.addParam(AnalyticsEvent.USER_DETAILS.NAME,viewModel.countryCode+viewModel.phoneNumber)
                    appAnalytics.push()
                    if (activityResultFlag) {
                        setResult()
                        return@Observer
                    }
                    val intent = getIntentForState()
                    if (intent == null) {
                        //AppAnalytics.create(AnalyticsEvent.LOGIN_SUCCESS.NAME).push()
                        startActivity(getInboxActivityIntent())
                    } else {
                        startActivity(intent)
                    }
                    finish()
                    return@Observer
                }
                SignUpStepStatus.SignUpWithoutRegister -> {
                    appAnalytics.addParam(AnalyticsEvent.STATUS.NAME,AnalyticsEvent.SUCCESS_PARAM.NAME)
                    if(viewModel.phoneNumber.isEmpty().not())
                        appAnalytics.addParam(AnalyticsEvent.USER_DETAILS.NAME,viewModel.countryCode+viewModel.phoneNumber)
                    appAnalytics.push()
                    openCourseExplorerScreen(this@SignUpActivity)
                    return@Observer
                }
                else -> return@Observer
            }
        })
    }

    private fun addNextFragment() {
        val signUpStep2Fragment: SignUpStep2Fragment =
            SignUpStep2Fragment.newInstance()
        val slideTransition = androidx.transition.Slide(Gravity.END)
        slideTransition.duration = 200
        val changeBoundsTransition = ChangeBounds()
        changeBoundsTransition.duration = 200
        signUpStep2Fragment.enterTransition = slideTransition
        signUpStep2Fragment.sharedElementEnterTransition = changeBoundsTransition
        signUpStep2Fragment.allowEnterTransitionOverlap = false
        signUpStep2Fragment.allowReturnTransitionOverlap = false

        supportFragmentManager.commit(true) {
            addToBackStack(signUpStep2Fragment::class.java.name)
            replace(
                R.id.container,
                signUpStep2Fragment,
                signUpStep2Fragment::class.java.name
            )
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            appAnalytics.addParam(AnalyticsEvent.STATUS.NAME,AnalyticsEvent.CANCELLED_PARAM.NAME)
            appAnalytics.push()
            this@SignUpActivity.finish()
            return
        }
        super.onBackPressed()
    }
}
