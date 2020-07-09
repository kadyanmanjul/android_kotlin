package com.joshtalks.joshskills.ui.assessment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.databinding.ActivityAssessmentBinding

class AssessmentActivity : CoreJoshActivity() {

    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AssessmentViewModel::class.java) }
    private var assessmentId: Int = 0
    private var flowFrom: String? = null

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
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.black)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_assessment)
        binding.lifecycleOwner = this
        binding.handler = this
        assessmentId = intent.getIntExtra(AssessmentActivity.KEY_ASSESSMENT_ID, 0)
        if (intent.hasExtra(STARTED_FROM)) {
            flowFrom = intent.getStringExtra(STARTED_FROM)
        }
        if (assessmentId != 0) {
            getAssessmentDetails(assessmentId)
        } else {
            finish()
        }
    }

    private fun getAssessmentDetails(assessmentId: Int) {
        viewModel.fetchAssessmentDetails(assessmentId)
    }

    companion object {
        const val KEY_ASSESSMENT_ID = "assessment-id"

        fun startAssessmentActivity(
            activity: Activity,
            assessmentId: Int,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, AssessmentActivity::class.java).apply {
                putExtra(KEY_ASSESSMENT_ID, assessmentId)
                if (startedFrom.isNotBlank())
                    putExtra(STARTED_FROM, startedFrom)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }

        fun getIntent(
            context: Context,
            assessmentId: Int,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, AssessmentActivity::class.java).apply {
            putExtra(KEY_ASSESSMENT_ID, assessmentId)
            if (startedFrom.isNotBlank())
                putExtra(STARTED_FROM, startedFrom)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }
}
