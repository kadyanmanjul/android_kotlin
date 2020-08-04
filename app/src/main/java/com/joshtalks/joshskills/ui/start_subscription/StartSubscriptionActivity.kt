package com.joshtalks.joshskills.ui.start_subscription

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.databinding.ActivityStartSubscriptionBinding
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import timber.log.Timber

class StartSubscriptionActivity : BaseActivity() {

    private lateinit var binding: ActivityStartSubscriptionBinding
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

        if (testId == 0) {
            finish()
        }

        when (exploreCardType) {

            ExploreCardType.NORMAL -> {
                Timber.tag("")
            }

            ExploreCardType.FREETRIAL -> {
                binding.startBtn.text =
                    AppObjectController.getFirebaseRemoteConfig().getString("start_7_day_trial")
            }

            ExploreCardType.SUBSCRIPTION -> {
                binding.startBtn.text =
                    AppObjectController.getFirebaseRemoteConfig().getString("start_subscription")
            }

            ExploreCardType.FFCOURSE -> {
                binding.startBtn.text =
                    AppObjectController.getFirebaseRemoteConfig().getString("start_free_course")
            }

        }

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
