package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.signup_v2.FLOW_FROM
import com.joshtalks.joshskills.ui.signup_v2.SignUpV2Activity


class OnBoardActivity : CoreJoshActivity() {
    private lateinit var layout: ActivityOnboardBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_onboard
        )
        layout.handler = this
        layout.lifecycleOwner = this
    }

    fun signUp() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
            .push()
        val intent = Intent(this, SignUpV2Activity::class.java).apply {
            putExtra(FLOW_FROM, "onboarding journey")
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
}
