package com.joshtalks.joshskills.ui.subscription

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityStartSubscriptionBinding
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity

class StartSubscriptionActivity : BaseActivity() {

    private lateinit var binding: ActivityStartSubscriptionBinding
    private val viewModel by lazy { ViewModelProvider(this).get(StartSubscriptionViewModel::class.java) }
    private var testId: Int = 0
    private var exploreCardType = ExploreCardType.NORMAL
    private var startedFrom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start_subscription)
        binding.lifecycleOwner = this
        binding.handler = this

        testId = intent.getIntExtra(KEY_TEST_ID, 0)
        exploreCardType = ExploreCardType.valueOf(intent.getStringExtra(KEY_EXPLORE_CARD_TYPE)!!)
        if (intent.hasExtra(STARTED_FROM)) {
            startedFrom = intent.getStringExtra(STARTED_FROM)
        }

        when (exploreCardType) {

            ExploreCardType.NORMAL -> {
                finish()
            }

            ExploreCardType.FREETRIAL -> {
                logEvent(AnalyticsEvent.SEVEN_DAY_POPUP.name)
                binding.startBtn.text =
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.START_TRIAL_CTA_LABEL)
                binding.txtHeading.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.START_TRAIL_TITLE)
                binding.txtHeadingMain.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.START_TRAIL_HEADING)
                binding.item1Txt.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.START_TRAIL_FEATURE1)
                binding.item2Txt.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.START_TRAIL_FEATURE2)
                binding.item3Txt.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.START_TRAIL_FEATURE3)
            }

            ExploreCardType.FFCOURSE -> {
                binding.startBtn.text =
                    AppObjectController.getFirebaseRemoteConfig().getString("start_free_course")
            }
        }

        addObservers()

    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun addObservers() {
        binding.startBtn.setOnClickListener {
            when (exploreCardType) {

                ExploreCardType.NORMAL -> {
                    finish()
                }

                ExploreCardType.FREETRIAL -> {
                    logEvent(AnalyticsEvent.SEVEN_DAY_CLICKED.NAME)
                    getSubscriptionDetails()
                }

                ExploreCardType.FFCOURSE -> {
                    PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString())
                }
            }
        }

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
        })

        viewModel.subscriptionTestDetailsLiveData.observe(this, Observer {
            it.id?.toString()?.let { testId ->
                PaymentSummaryActivity.startPaymentSummaryActivity(
                    this,
                    testId,
                    hasFreeTrial = true
                )
            }
        })

    }

    private fun getSubscriptionDetails() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchSubscriptionDetails()
    }

    fun cancel() {
        finish()
    }

    companion object {
        private const val KEY_TEST_ID = "test-id"
        private const val KEY_EXPLORE_CARD_TYPE = "explore-card-type"

        fun startActivity(
            activity: Activity,
            testId: Int,
            exploreCardType: ExploreCardType,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, StartSubscriptionActivity::class.java).apply {
                putExtra(KEY_TEST_ID, testId)
                putExtra(KEY_EXPLORE_CARD_TYPE, exploreCardType.name)
                if (startedFrom.isNotBlank())
                    putExtra(STARTED_FROM, startedFrom)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }

}
