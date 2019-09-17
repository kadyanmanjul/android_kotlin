package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.REDIRECT_URL
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityRegisterInfoBinding

class RegisterInfoActivity : BaseActivity(){

    private lateinit var layout: ActivityRegisterInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_register_info)
        layout.handler=this
        supportActionBar?.hide()
        AppAnalytics.create(AnalyticsEvent.COURSE_FAILURE_SCREEN.NAME).push()

    }


    fun clickToPay(){
        AppAnalytics.create(AnalyticsEvent.CLICK_TO_PAY_SELECTED.NAME).push()
        Utils.openUrl(REDIRECT_URL)
        //this.finish()
    }

    fun callHelpLine(){
        AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME).push()

        Utils.call(this,"7428797127")
        //this.finish()
    }

    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()

        super.onBackPressed()
        val resultIntent = Intent()
        setResult(RESULT_OK, resultIntent)
        finish()
    }

}