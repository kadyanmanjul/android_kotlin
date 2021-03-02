package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REFERRED_REFERRAL_CODE
import com.joshtalks.joshskills.core.USER_LOCALE_UPDATED
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
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
        VersionResponse.getInstance().version.name?.let {
            if (it == ONBOARD_VERSIONS.ONBOARDING_V7 && AppObjectController.isSettingUpdate.not() && PrefManager.getBoolValue(
                    USER_LOCALE_UPDATED
                ).not()
            ) {
                openLanguageChooserDialog()
            }
        }
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
        /*
        val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, false)

        if (exploreType.isNotBlank()) {
            WorkManagerAdmin.registerUserGAID(null, exploreType)
        } else {
            WorkManagerAdmin.registerUserGAID(null, null)
        }
*/
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

    private fun openLanguageChooserDialog() {
        val dialog = MaterialDialog(this)
            .customView(R.layout.language_select_layout, scrollable = true)
        val customView = dialog.getCustomView()
        val tColor = ContextCompat.getColor(this, R.color.colorAccent)

        val text = SpannableStringBuilder()
            .append("Pick your ")
            .color(tColor) { append("Language") }
        customView.findViewById<AppCompatTextView>(R.id.title).text = text
        customView.findViewById<View>(R.id.tv_hindi).setOnClickListener {
            requestForChangeLocale("hi", dialog)
        }
        customView.findViewById<View>(R.id.tv_english).setOnClickListener {
            requestForChangeLocale("en", dialog)
        }
        dialog.show()
    }

    private fun requestForChangeLocale(language: String, dialog: MaterialDialog) {
        FullScreenProgressDialog.showProgressBar(this)
        requestWorkerForChangeLanguage(language, successCallback = {
            dialog.dismiss()
            FullScreenProgressDialog.hideProgressBar(this)
        }, errorCallback = {
            FullScreenProgressDialog.hideProgressBar(this)
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@OnBoardActivity.finish()
    }

}
