package com.joshtalks.joshskills.ui.certification_exam.report.vh

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
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
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenReportQTypeEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.repository.server.certification_exam.QuestionReportType
import com.joshtalks.joshskills.repository.server.certification_exam.UserSelectedAnswer
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


@SuppressLint("NonConstantResourceId")
@Layout(R.layout.layout_report_overview_view3)
class ReportOverviewView3(
    private val certificateExamReport: CertificateExamReportModel,
    private val totalQuestions: List<CertificationQuestion>,
    private val reportType: QuestionReportType
) {

    @com.mindorks.placeholderview.annotations.View(R.id.chart)
    lateinit var chart: PieChart

    @com.mindorks.placeholderview.annotations.View(R.id.recycler_view)
    lateinit var questionRecyclerView: RecyclerView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_correct)
    lateinit var tvCorrect: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_incorrect)
    lateinit var tvIncorrect: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_unanswered)
    lateinit var tvUnanswered: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.ll_unanswerd)
    lateinit var llUnAnswered: LinearLayout

    @com.mindorks.placeholderview.annotations.View(R.id.ll_incorrect)
    lateinit var llIncorrect: LinearLayout

    @com.mindorks.placeholderview.annotations.View(R.id.ll_correct)
    lateinit var llcorrect: LinearLayout


    private val context: Context = AppObjectController.joshApplication

    @Resolve
    fun onViewInflated() {
        certificateExamReport.run {
            tvCorrect.text = correct.toString()
            tvIncorrect.text = wrong.toString()
            tvUnanswered.text = unanswered.toString()
            when (reportType) {
                QuestionReportType.RIGHT -> {
                    llcorrect.visibility = View.VISIBLE
                }
                QuestionReportType.WRONG -> {
                    llIncorrect.visibility = View.VISIBLE
                }
                QuestionReportType.UNANSWERED -> {
                    llUnAnswered.visibility = View.VISIBLE
                }
            }
            setChart()
            setQuestionRV()
        }
    }

    private fun setQuestionRV() {
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
            questionRecyclerView.apply {
                addItemDecoration(GridSpacingItemDecoration(8, Utils.dpToPx(context, 6f), true))
                setHasFixedSize(true)
                layoutManager = GridLayoutManager(context, 8)
                adapter = ReportQuestionListAdapter(answersList, reportType)
            }

        }
    }

    private fun getAnswerStatus(certificationQuestion: CertificationQuestion): Boolean? {
        return certificateExamReport.answers?.find { it.question == certificationQuestion.questionId }?.isAnswerCorrect
    }

    private fun setChart() {
        CoroutineScope(Dispatchers.Main).launch {
            chart.setCenterTextTypeface(
                Typeface.createFromAsset(
                    context.assets,
                    "fonts/OpenSans-SemiBold.ttf"
                )
            )
            chart.setCenterTextSize(12F)
            chart.setExtraOffsets(0F, 0F, 0F, -15F)
            chart.holeRadius = 58f
            chart.transparentCircleRadius = 58F
            chart.isDrawHoleEnabled = true
            chart.setHoleColor(Color.TRANSPARENT)
            chart.setTransparentCircleColor(Color.TRANSPARENT)
            chart.animateY(0, EaseInOutQuad)
            chart.isRotationEnabled = true
            chart.isHighlightPerTapEnabled = true
            chart.setDrawEntryLabels(false)
            chart.description.isEnabled = false
            val legend: Legend = chart.legend
            legend.formSize = 0F

            val percentData = arrayListOf<PieEntry>()
            percentData.add(PieEntry(certificateExamReport.correct.toFloat(), 0))
            percentData.add(PieEntry(certificateExamReport.wrong.toFloat(), 1))
            percentData.add(PieEntry(certificateExamReport.unanswered.toFloat(), 2))

            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                percentData.clear()
                percentData.add(PieEntry(certificateExamReport.unanswered.toFloat(), 0))
            }
            val dataSet = PieDataSet(percentData, "")
            var colorCorrect: Int = ContextCompat.getColor(context, R.color.transparent)
            var colorInCorrect: Int = ContextCompat.getColor(context, R.color.transparent)
            var colorInUnAnswered: Int = ContextCompat.getColor(context, R.color.transparent)

            val textColor: Int
            val percentText: String
            when {
                QuestionReportType.WRONG == reportType -> {
                    colorInCorrect = Color.parseColor("#F6595A")
                    textColor = colorInCorrect
                    percentText = EMPTY + getPercent(certificateExamReport.wrong) + "% Incorrect"
                }
                QuestionReportType.UNANSWERED == reportType -> {
                    colorInUnAnswered = ContextCompat.getColor(context, R.color.grey_68)
                    textColor = colorInUnAnswered
                    percentText =
                        EMPTY + getPercent(certificateExamReport.unanswered) + "% Unanswered"

                }
                else -> {
                    colorCorrect = Color.parseColor("#3DD2B5")
                    textColor = colorCorrect
                    percentText = EMPTY + getPercent(certificateExamReport.correct) + "% Correct"
                }
            }
            chart.setCenterTextColor(textColor)
            dataSet.colors = mutableListOf(colorCorrect, colorInCorrect, colorInUnAnswered)
            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                dataSet.color = ContextCompat.getColor(context, R.color.grey_68)
            }
            chart.centerText = generateCenterSpannableText(percentText)

            dataSet.setDrawValues(false)
            dataSet.sliceSpace = 0f

            val data = PieData(dataSet)
            chart.data = data
            data.setDrawValues(false)
            //chart.highlightValue(0f, 0, false)
        }
    }


    private fun generateCenterSpannableText(text: String): SpannableString {
        val span = SpannableString(text)
        val i = text.indexOf("%") + 1
        span.setSpan(RelativeSizeSpan(1.75f), 0, i, 0)
        return span
    }

    private fun getPercent(per: Int): Float {
        return (per.toFloat() * 100) / (certificateExamReport.correct + certificateExamReport.wrong + certificateExamReport.unanswered)
    }

    @Click(R.id.root_view)
    fun onClickRootView() {
        RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNKNOWN))
    }

}
