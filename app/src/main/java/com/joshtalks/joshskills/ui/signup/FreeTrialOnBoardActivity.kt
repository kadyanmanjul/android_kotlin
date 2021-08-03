package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityFreeTrialOnBoardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FreeTrialOnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityFreeTrialOnBoardBinding
    private val viewModel: FreeTrialViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_on_board
        )
        layout.handler = this
        layout.lifecycleOwner = this
        addObserver()
    }

    private fun addObserver() {
        viewModel.apiStatus.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                   moveToInboxScreen()
                }
                else -> {
                   //showToast(getString(R.string.something_went_wrong))
                }
            }}
    }

    private fun moveToInboxScreen() {
        AppAnalytics.create(AnalyticsEvent.FREE_TRIAL_ONBOARDING.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .push()
        val intent = Intent(this@FreeTrialOnBoardActivity, InboxActivity::class.java).apply {
            putExtra(FLOW_FROM, "free trial onboarding journey")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    fun signUp() {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
                .push()
            val intent = Intent(this@FreeTrialOnBoardActivity, SignUpActivity::class.java).apply {
                putExtra(FLOW_FROM, "free trial onboarding journey")
            }
            startActivity(intent)
        }
    }

    fun startTrial() {
        if (Mentor.getInstance().getId().isNotEmpty()){
            viewModel.startFreeTrial(Mentor.getInstance().getId())
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@FreeTrialOnBoardActivity.finish()
    }

}
