package com.joshtalks.joshskills.common.ui.certification_exam.report.vh

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.DownloadFileEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.ui.certification_exam.constants.EXAM_TYPE_ADVANCED
import com.joshtalks.joshskills.common.ui.certification_exam.constants.EXAM_TYPE_BEGINNER
import com.joshtalks.joshskills.common.ui.certification_exam.constants.EXAM_TYPE_INTERMEDIATE
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import java.text.DecimalFormat

const val BTN_CHANGED_TEXT = "Show Certificate"

@SuppressLint("NonConstantResourceId")
@Layout(R.layout.layout_report_overview_view1)
class ReportOverviewView1(private val certificateExamReport: CertificateExamReportModel, private val examType: String?) {

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

    @Resolve
    fun onViewInflated() {
        Log.i(TAG, "onViewInflated: triggred")
        certificateExamReport.run {
            headerTv.text = heading
            resultInfo.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (isExamPass) { //change for test
                cDownloadGroup.visibility = View.VISIBLE
                checkExamDetails.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_500
                    )
                )
                checkExamDetails.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.pure_white
                    )
                )

                checkExamDetails.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_500
                    )
                )

                checkExamDetails.strokeWidth = 2

                checkExamDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)

                if (certificateURL.isNullOrEmpty()) {
                    downloadCertificateBtn.visibility = View.GONE
                }
                var showBtn = false
                when (examType) {
                    EXAM_TYPE_BEGINNER -> {
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_BEGINNER, defValue = false)
                    }
                    EXAM_TYPE_INTERMEDIATE -> {
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_INTERMEDIATE, defValue = false)
                    }
                    EXAM_TYPE_ADVANCED -> {
                        showBtn = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_ADVANCED, defValue = false)
                    }
                }
                if (showBtn) {
                    downloadCertificateBtn.text = BTN_CHANGED_TEXT
                    checkExamDetails.visibility = View.VISIBLE
                }else{
                    checkExamDetails.visibility = View.GONE
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
        if (Utils.isInternetAvailable()){
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                DownloadFileEventBus(
                    id = certificateExamReport.reportId,
                    url = certificateExamReport.certificateURL
                )
            )
        }else{
            showToast("No Internet Available")
        }
    }

    @Click(R.id.check_exam_details)
    fun checkExamDetails() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(EmptyEventBus())
    }
}
