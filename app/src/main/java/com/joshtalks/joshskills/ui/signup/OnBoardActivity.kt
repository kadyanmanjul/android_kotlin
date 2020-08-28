package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REFERRED_REFERRAL_CODE
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.referral.EnterReferralCodeFragment


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
        if (PrefManager.getStringValue(REFERRED_REFERRAL_CODE).isBlank())
            layout.haveAReferralCode.visibility = View.VISIBLE
    }

    fun signUp() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .push()
        val intent = Intent(this, SignUpActivity::class.java).apply {
            putExtra(FLOW_FROM, "onboarding journey")
        }
        startActivity(intent)
    }

    fun openCourseExplore() {
        val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, false)
        if (exploreType.isNotBlank()) {
            WorkManagerAdmin.registerUserGAID(null, exploreType)
        } else {
            WorkManagerAdmin.registerUserGAID(null, null)
        }

        AppAnalytics.create(AnalyticsEvent.EXPLORE_BTN_CLICKED.NAME)
            .addParam("name", this.javaClass.simpleName)
            .addBasicParam()
            .addUserDetails()
            .push()
        startActivity(Intent(applicationContext, CourseExploreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    fun openReferralDialogue() {

        val bottomSheetFragment = EnterReferralCodeFragment.newInstance()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@OnBoardActivity.finish()
    }
}
