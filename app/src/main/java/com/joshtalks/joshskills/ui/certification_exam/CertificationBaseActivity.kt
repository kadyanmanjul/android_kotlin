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
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.ui.certification_exam.examview.CExamMainActivity
import com.joshtalks.joshskills.ui.certification_exam.report.CExamReportActivity
import com.joshtalks.joshskills.ui.certification_exam.view.InstructionFragment
import com.joshtalks.joshskills.ui.pdfviewer.MESSAGE_ID
import kotlinx.android.synthetic.main.activity_certification_base.progress_bar
import kotlinx.android.synthetic.main.inbox_toolbar.iv_back
import kotlinx.android.synthetic.main.inbox_toolbar.text_message_title

const val CERTIFICATION_EXAM_ID = "certification_exam_ID"
const val CERTIFICATION_EXAM_QUESTION = "certification_exam_question"
const val CURRENT_QUESTION = "current_question"
const val EXAM_STATUS = "current_question"

class CertificationBaseActivity : BaseActivity() {

    companion object {
        fun certificationExamIntent(
            activity: Activity,
            conversationId: String,
            chatMessageId: String,
            certificationId: Int,
            cExamStatus: CExamStatus = CExamStatus.FRESH
        ): Intent {
            return Intent(activity, CertificationBaseActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(MESSAGE_ID, chatMessageId)
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(CERTIFICATION_EXAM_ID, certificationId)
                putExtra(EXAM_STATUS, cExamStatus)
            }
        }
    }

    private var certificateExamId: Int = -1
    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var isSubmittedExamTest = false
    private var cExamStatus: CExamStatus = CExamStatus.FRESH

    private var openExamActivityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isSubmittedExamTest = true
            viewModel.getQuestions(certificateExamId)
            viewModel.isUserSubmitExam.postValue(true)
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
        cExamStatus = intent.getSerializableExtra(EXAM_STATUS) as CExamStatus
        intent.getStringExtra(CONVERSATION_ID)?.let {
            viewModel.conversationId = it
        }

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
                viewModel.showPreviousResult()
            }
            if (CExamStatus.REATTEMPTED == cExamStatus) {
                viewModel.startExam()
                cExamStatus = CExamStatus.NIL
            } else if (CExamStatus.CHECK_RESULT == cExamStatus) {
                viewModel.showPreviousResult()
                cExamStatus = CExamStatus.NIL
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
        if (viewModel.isUserSubmitExam.value != null && viewModel.isUserSubmitExam.value!!) {
            val resultIntent = Intent().apply {
                putExtra(MESSAGE_ID, intent.getStringExtra(MESSAGE_ID))
            }
            setResult(RESULT_OK, resultIntent)
            this.finish()
        } else {
            this.finish()
        }
    }
}