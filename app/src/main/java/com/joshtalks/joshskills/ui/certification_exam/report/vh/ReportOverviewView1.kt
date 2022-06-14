package com.joshtalks.joshskills.ui.certification_exam.report.vh

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DownloadFileEventBus
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.ui.certification_exam.constants.EXAM_TYPE_ADVANCED
import com.joshtalks.joshskills.ui.certification_exam.constants.EXAM_TYPE_BEGINNER
import com.joshtalks.joshskills.ui.certification_exam.constants.EXAM_TYPE_INTERMEDIATE
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import java.text.DecimalFormat

const val BTN_CHANGED_TEXT = "Show Certificate"
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

    @com.mindorks.placeholderview.annotations.View(R.id.btn_download_certificate)
    lateinit var downloadCertificateBtn: MaterialButton

    private val context: Context = AppObjectController.joshApplication

    private var examType = String()

    @Resolve
    fun onViewInflated() {
        certificateExamReport.run {
            headerTv.text = heading
            resultInfo.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (isExamPass) { //change for test
                cDownloadGroup.visibility = View.VISIBLE
                checkExamDetails.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.blue_btn_text_check_exam_details
                    )
                )
                checkExamDetails.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )

                checkExamDetails.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.blue_btn_text_check_exam_details
                    )
                )

                checkExamDetails.strokeWidth = 2

                checkExamDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP,12F)

                if (certificateURL.isNullOrEmpty()) {
                    downloadCertificateBtn.visibility = View.GONE
                }
                var showBtn = false
                when (examType){
                    EXAM_TYPE_BEGINNER->{
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_BEGINNER, defValue = true)
                    }
                    EXAM_TYPE_INTERMEDIATE->{
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_INTERMEDIATE, defValue = true)
                    }
                    EXAM_TYPE_ADVANCED->{
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_ADVANCED, defValue = true)
                    }
                }
                if (showBtn) {
                    checkExamDetails.visibility = View.GONE
                }else{
                    downloadCertificateBtn.text = BTN_CHANGED_TEXT
                    checkExamDetails.visibility = View.VISIBLE
                }
            }

            scoreTv.text = getScoreText(score, maxScore)
        }
    }

    private fun getScoreText(score: Double, maxScore: Int): SpannableString? {
        val string1 = context.getString(R.string.your_score)
        val format = DecimalFormat("0.#") // to remove trailing zeroes
        val formattedScore = format.format(score)
        val string2 = formattedScore.toString().plus(" / ").plus(maxScore.toString())
        val s = SpannableString(string1 + "\n" + string2)
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
        RxBus2.publish(
            DownloadFileEventBus(
                id = certificateExamReport.reportId,
                url = certificateExamReport.certificateURL
            )
        )
    }

    @Click(R.id.check_exam_details)
    fun checkExamDetails() {
        RxBus2.publish(EmptyEventBus())
    }

    fun checkExamType(certificateExamType: String) {
        examType = certificateExamType
    }
}
