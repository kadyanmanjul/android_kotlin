package com.joshtalks.joshskills.ui.certification_exam.report.vh

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing.EaseInOutQuad
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.repository.server.certification_exam.UserSelectedAnswer
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@SuppressLint("NonConstantResourceId")
@Layout(R.layout.layout_report_overview_view2)
class ReportOverviewView2(
    private val certificateExamReport: CertificateExamReportModel,
    private val totalQuestions: List<CertificationQuestion>
) {

    @com.mindorks.placeholderview.annotations.View(R.id.chart)
    lateinit var chart: PieChart

    @com.mindorks.placeholderview.annotations.View(R.id.recycler_view)
    lateinit var questionRecyclerView: RecyclerView

    private val context: Context = AppObjectController.joshApplication
    @Resolve
    fun onViewInflated() {
        certificateExamReport.run {
            setChart()
            setQuestionRV()
        }
    }

    private fun setQuestionRV() {
        CoroutineScope(Dispatchers.IO).launch {
            if (questionRecyclerView.adapter == null) {
                val answersList = ArrayList<UserSelectedAnswer>()
                totalQuestions.sortedBy { it.sortOrder }.forEach {
                    val status = getAnswerStatus(it)
                    answersList.add(
                        UserSelectedAnswer(
                            it.questionId,
                            -1,
                            isAnswerCorrect = status ?: false,
                            isNotAttempt = status
                        )
                    )
                }
                CoroutineScope(Dispatchers.Main).launch {
                    val layoutManager = GridLayoutManager(context, 8)
                    questionRecyclerView.addItemDecoration(
                        GridSpacingItemDecoration(8, Utils.dpToPx(context, 6f), true)
                    )
                    questionRecyclerView.apply {
                        setHasFixedSize(true)
                        setLayoutManager(layoutManager)
                    }
                    questionRecyclerView.adapter =
                        ReportQuestionListAdapter(answersList)
                }
            }
        }

    }

    private fun getAnswerStatus(certificationQuestion: CertificationQuestion): Boolean? {
        return certificateExamReport.answers?.find { it.question == certificationQuestion.questionId }?.isAnswerCorrect
    }

    private fun setChart() {
        CoroutineScope(Dispatchers.Main).launch {
            chart.centerText = generateCenterSpannableText(certificateExamReport.percent)
            chart.setCenterTextColor(Color.parseColor("#38B099"))
            chart.setCenterTextTypeface(
                Typeface.createFromAsset(
                    context.assets,
                    "fonts/OpenSans-SemiBold.ttf"
                )
            )
            chart.setCenterTextSize(14F)
            chart.setExtraOffsets(0F, 0F, 0F, -10F)
            chart.holeRadius = 58f
            chart.transparentCircleRadius = 58F
            chart.isDrawHoleEnabled = true
            chart.setHoleColor(Color.WHITE)
            chart.setTransparentCircleColor(Color.WHITE)
            chart.animateY(500, EaseInOutQuad)
            chart.isRotationEnabled = true
            chart.isHighlightPerTapEnabled = true
            chart.setDrawEntryLabels(false)
            chart.description.isEnabled = false
            val legend: Legend = chart.legend
            legend.formSize = 0F

            val percentData = arrayListOf<PieEntry>()
            percentData.add(PieEntry(certificateExamReport.correct.toFloat(), 0))
            percentData.add(PieEntry(certificateExamReport.wrong.toFloat(), 1))

            val dataSet = PieDataSet(percentData, "")
            val colorCorrect = Color.parseColor("#3DD2B5")
            val colorInCorrect = Color.parseColor("#F6595A")
            dataSet.colors = mutableListOf(colorCorrect, colorInCorrect)
            dataSet.setDrawValues(false)
            dataSet.sliceSpace = 0f

            val data = PieData(dataSet)
            chart.data = data
            data.setDrawValues(false)
            //chart.highlightValue(0f, 0, false)
        }
    }

    private fun generateCenterSpannableText(percent: Float): SpannableString? {
        val s0 = "$percent%"
        val span = SpannableString("$s0 Correct")
        span.setSpan(RelativeSizeSpan(1.65f), 0, s0.length, 0)
        return span
    }
}
