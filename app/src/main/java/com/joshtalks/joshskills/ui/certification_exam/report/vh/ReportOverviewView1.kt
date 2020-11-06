package com.joshtalks.joshskills.ui.certification_exam.report.vh


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@SuppressLint("NonConstantResourceId")
@Layout(R.layout.layout_report_overview_view1)
class ReportOverviewView1(private val certificateExamReport: CertificateExamReportModel) {

    @com.mindorks.placeholderview.annotations.View(R.id.header_tv)
    lateinit var headerTv: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.score_tv)
    lateinit var scoreTv: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.result_info)
    lateinit var resultInfo: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.check_exam_details)
    lateinit var checkExamDetails: MaterialButton

    @com.mindorks.placeholderview.annotations.View(R.id.group_certificate_download)
    lateinit var cDownloadGroup: androidx.constraintlayout.widget.Group

    private val context: Context = AppObjectController.joshApplication

    @Resolve
    fun onViewInflated() {
        certificateExamReport.run {
            headerTv.text = heading
            resultInfo.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (isExamPass) {
                cDownloadGroup.visibility = View.VISIBLE
                checkExamDetails.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorAccent
                    )
                )
                checkExamDetails.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
            }
            checkExamDetails.visibility = View.VISIBLE
            scoreTv.text = getScoreText(score, maxScore)
        }
    }

    private fun getScoreText(score: Double, maxScore: Int): SpannableString? {
        val string1 = context.getString(R.string.your_score)
        val string2 = score.toString().plus("/").plus(maxScore.toString())
        val s = SpannableString(string1 + string2)
        s.setSpan(
            AbsoluteSizeSpan(24, true),
            string1.length,
            s.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return s
    }

    @Click(R.id.btn_download_certificate)
    fun downloadCertificate() {

    }

    @Click(R.id.check_exam_details)
    fun checkExamDetails() {
        RxBus2.publish(EmptyEventBus())
    }
}
