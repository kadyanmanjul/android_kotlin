package com.joshtalks.joshskills.ui.assessment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshcamerax.utils.onPageSelected
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.loadJSONFromAsset
import com.joshtalks.joshskills.databinding.ActivityAssessmentBinding
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.viewholder.AssessmentQuestionAdapter

class AssessmentActivity : CoreJoshActivity() {

    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AssessmentViewModel::class.java) }
    private var assessmentId: Int = 0
    private var flowFrom: String? = null
    private val hintOptionsSet = mutableSetOf<ChoiceType>()


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
        test()
        /*
        if (assessmentId != 0) {
            getAssessmentDetails(assessmentId)
        } else {
            finish()
        }*/
        subscribeLiveData()
    }

    private fun getAssessmentDetails(assessmentId: Int) {
        viewModel.fetchAssessmentDetails(assessmentId)
    }

    private fun subscribeLiveData() {
        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
        })

        viewModel.assessmentLiveData.observe(this, Observer { assessmentResponse ->
            // TODO Bind view
        })
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

    fun test() {
        val assessmentResponse = AppObjectController.gsonMapperForLocal.fromJson(
            loadJSONFromAsset("assessmentJson.json"),
            AssessmentResponse::class.java
        )

        val data = AssessmentWithRelations(assessmentResponse)

        // binding.questionViewPager.offscreenPageLimit=1
        val adapter = AssessmentQuestionAdapter(
            data.assessment.type,
            data.assessment.status,
            AssessmentQuestionViewType.CORRECT_ANSWER_VIEW,
            data.questionList
        )
        binding.questionViewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.questionViewPager) { tab, position ->
        }.attach()
        binding.questionViewPager.setPageTransformer(
            MarginPageTransformer(
                Utils.dpToPx(
                    applicationContext,
                    16f
                )
            )

        )
        binding.questionViewPager.onPageSelected { position ->
            val type = assessmentResponse.questions[position].choiceType
            if (hintOptionsSet.contains(type).not()) {
                assessmentResponse.intro.find { it.type == type }?.run {
                    IntroQuestionFragment.newInstance(this)
                        .show(supportFragmentManager, "Question Tip")
                    hintOptionsSet.add(type)
                }
            }
        }

    }
}
