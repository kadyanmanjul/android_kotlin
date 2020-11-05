package com.joshtalks.joshskills.ui.certification_exam.report.vh


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing.EaseInOutQuad
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.utils.ColorTemplate
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
        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.setExtraOffsets(5f, 10f, 5f, 5f)

        chart.dragDecelerationFrictionCoef = 0.95f

        //    chart.setCenterTextTypeface(tfLight)
        chart.centerText = generateCenterSpannableText()

        chart.isDrawHoleEnabled = true
        chart.setHoleColor(Color.WHITE)

        chart.setTransparentCircleColor(Color.WHITE)
        chart.setTransparentCircleAlpha(110)

        chart.holeRadius = 58f
        chart.transparentCircleRadius = 61f

        chart.setDrawCenterText(true)

        chart.rotationAngle = 0f
        // enable rotation of the chart by touch
        // enable rotation of the chart by touch
        chart.isRotationEnabled = true
        chart.isHighlightPerTapEnabled = true

        // chart.setUnit(" €");
        // chart.setDrawUnitsInChart(true);

        // add a selection listener

        // chart.setUnit(" €");
        // chart.setDrawUnitsInChart(true);

        chart.animateY(1400, EaseInOutQuad)
        // chart.spin(2000, 0, 360);

        // chart.spin(2000, 0, 360);
        val l = chart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(false)
        l.xEntrySpace = 7f
        l.yEntrySpace = 0f
        l.yOffset = 0f

        // entry label styling

        // entry label styling
        chart.setEntryLabelColor(Color.WHITE)
        //       chart.setEntryLabelTypeface(tfRegular)
        chart.setEntryLabelTextSize(12f)
    }


    private fun getScoreText(score: Double, maxScore: Int): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder()
        val string1 = context.getString(R.string.your_score)
        val span1 = SpannableString(string1)
        span1.setSpan(AbsoluteSizeSpan(R.dimen._14ssp), 0, string1.length, 0)
        spannableStringBuilder.append(span1)

        val string2 = score.toString().plus(maxScore.toString())

        val span2 = SpannableString(string2)
        span2.setSpan(AbsoluteSizeSpan(R.dimen._20ssp), 0, string2.length, 0)
        spannableStringBuilder.append(span2)
        return spannableStringBuilder
    }


    private fun generateCenterSpannableText(): SpannableString? {
        val s = SpannableString("MPAndroidChart\ndeveloped by Philipp Jahoda")
        s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
        s.setSpan(StyleSpan(Typeface.NORMAL), 14, s.length - 15, 0)
        s.setSpan(ForegroundColorSpan(Color.GRAY), 14, s.length - 15, 0)
        s.setSpan(RelativeSizeSpan(.8f), 14, s.length - 15, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), s.length - 14, s.length, 0)
        s.setSpan(ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length - 14, s.length, 0)
        return s
    }
}
