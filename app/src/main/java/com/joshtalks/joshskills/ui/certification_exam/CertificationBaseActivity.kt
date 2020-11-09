package com.joshtalks.joshskills.ui.certification_exam

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.ui.certification_exam.examview.CExamMainActivity
import com.joshtalks.joshskills.ui.certification_exam.report.CExamReportActivity
import com.joshtalks.joshskills.ui.certification_exam.view.InstructionFragment
import kotlinx.android.synthetic.main.activity_certification_base.progress_bar
import kotlinx.android.synthetic.main.inbox_toolbar.iv_back
import kotlinx.android.synthetic.main.inbox_toolbar.text_message_title

const val CERTIFICATION_EXAM_ID = "certification_exam_ID"
const val CERTIFICATION_EXAM_QUESTION = "certification_exam_question"
const val CURRENT_QUESTION = "current_question"

class CertificationBaseActivity : BaseActivity() {

    companion object {
        fun certificationExamIntent(activity: Activity, certificationId: Int): Intent {
            return Intent(activity, CertificationBaseActivity::class.java).apply {
                putExtra(CERTIFICATION_EXAM_ID, certificationId)
            }
        }
    }

    private var certificateExamId: Int = 1
    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var isSubmittedExamTest = false

    private var openExamActivityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isSubmittedExamTest = true
            viewModel.getQuestions(certificateExamId)
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            viewModel.openResumeExam(certificateExamId)
        }
    }

    private var examReportActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certification_base)
        initView()
        addObserver()
        intent.getIntExtra(CERTIFICATION_EXAM_ID, -1).let {
            certificateExamId = it
        }
        certificateExamId = 1
        viewModel.getQuestions(certificateExamId)
    }

    private fun initView() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        text_message_title.text = getString(R.string.ce_header)
    }

    private fun addObserver() {
        viewModel.certificationQuestionLiveData.observe(this, {
            progress_bar.visibility = View.GONE
            openExamInstructionScreen()
            if (isSubmittedExamTest) {
                isSubmittedExamTest = false
                viewModel.previousResult()
            }
        })
        viewModel.startExamLiveData.observe(this, {
            viewModel.certificationQuestionLiveData.value?.let {
                openExamActivityResult.launch(CExamMainActivity.startExamActivity(this, it))
            }
        })
        viewModel.previousExamsResultLiveData.observe(this, {
            viewModel.certificationQuestionLiveData.value?.let {
                examReportActivityResult.launch(
                    CExamReportActivity.getExamResultActivityIntent(
                        this,
                        certificateExamId,
                        it
                    )
                )
            }
        })
    }


    private fun openExamInstructionScreen() {
        val prev = supportFragmentManager.findFragmentByTag(InstructionFragment::class.java.name)
        if (prev != null) {
            return
        }
        supportFragmentManager.commit(true) {
            addToBackStack(InstructionFragment::class.java.name)
            add(
                R.id.container,
                InstructionFragment.newInstance(),
                InstructionFragment::class.java.name
            )
        }
    }

    override fun onBackPressed() {
        this.finish()
    }
}