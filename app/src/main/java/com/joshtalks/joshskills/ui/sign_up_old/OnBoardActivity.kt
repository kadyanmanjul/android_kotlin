package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding
import io.github.inflationx.calligraphy3.TypefaceUtils
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import com.joshtalks.joshskills.core.BaseActivity


class OnBoardActivity : BaseActivity() {
    private lateinit var layout: ActivityOnboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_onboard
        )
        layout.handler = this
        AppAnalytics.create(AnalyticsEvent.LOGIN_SCREEN_1.NAME).push()


        val sBuilder = SpannableStringBuilder()
        sBuilder.append("Welcome to ").append("Josh Skills")
        val typefaceSpan =
            CalligraphyTypefaceSpan(TypefaceUtils.load(assets, "fonts/OpenSans-Bold.ttf"))
        sBuilder.setSpan(typefaceSpan, 11, 22, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        layout.textView.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }

    fun signUp() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_CLICKED.NAME).push()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

    }


    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
        this@OnBoardActivity.finish()

    }


}
