package com.joshtalks.joshskills.ui.certification_exam.result

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshcamerax.utils.onPageSelected
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.ui.certification_exam.CERTIFICATION_EXAM_ID
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.examview.CERTIFICATION_EXAM_QUESTION
import kotlinx.android.synthetic.main.activity_cexam_result.result_viewpager
import kotlinx.android.synthetic.main.activity_cexam_result.tab_layout

class CExamResultActivity : BaseActivity() {

    companion object {
        fun getExamResultActivityIntent(
            context: Context,
            certificateExamId: Int,
            certificationQuestionModel: CertificationQuestionModel,
        ): Intent {
            return Intent(context, CExamResultActivity::class.java).apply {
                putExtra(CERTIFICATION_EXAM_ID, certificateExamId)
                putExtra(CERTIFICATION_EXAM_QUESTION, certificationQuestionModel)
            }
        }
    }

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var certificateExamId: Int = -1
    private var certificationQuestionModel: CertificationQuestionModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cexam_result)
        certificateExamId = intent.getIntExtra(CERTIFICATION_EXAM_ID, -1)
        if (certificateExamId == -1) {
            this.finish()
            return
        }
        certificationQuestionModel =
            intent.getParcelableExtra(CERTIFICATION_EXAM_QUESTION) as CertificationQuestionModel?

        addObserver()
    }

    private fun addObserver() {

    }

    private fun getPreviousResult() {

    }

    private fun add() {
        result_viewpager.adapter = CExamResultAdapter(this)
        TabLayoutMediator(tab_layout, result_viewpager) { tab, position ->
            /*Do Nothing*/
        }.attach()

        result_viewpager.setPageTransformer(
            MarginPageTransformer(
                Utils.dpToPx(
                    applicationContext,
                    16f
                )
            )
        )
        val tabStrip: LinearLayout = tab_layout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { _, _ -> true }
        }

        result_viewpager.onPageSelected { position ->

        }

    }


}