package com.joshtalks.joshskills.certificate.report.vh

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.decorator.GridSpacingQuestionsDecoration
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenReportQTypeEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.common.repository.server.certification_exam.QuestionReportType
import com.joshtalks.joshskills.common.repository.server.certification_exam.UserSelectedAnswer
import com.mindorks.placeholderview.annotations.Resolve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("NonConstantResourceId")
class ReportOverviewView2(
    private val certificateExamReport: CertificateExamReportModel,
    private val totalQuestions: List<CertificationQuestion>
) {

    
    lateinit var chart: PieChart

    
    lateinit var questionRecyclerView: RecyclerView

    
    lateinit var tvCorrect: AppCompatTextView

    
    lateinit var tvIncorrect: AppCompatTextView

    
    lateinit var tvUnanswered: AppCompatTextView


    private val context: Context = AppObjectController.joshApplication

    @Resolve
    fun onViewInflated() {
        certificateExamReport.run {
            tvCorrect.text = correct.toString()
            tvIncorrect.text = wrong.toString()
            tvUnanswered.text = unanswered.toString()
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
                    val layoutManager = GridLayoutManager(context, 6,RecyclerView.HORIZONTAL,false)
                    questionRecyclerView.addItemDecoration(
                        GridSpacingQuestionsDecoration(6, Utils.dpToPx(context, 6f), true)
                    )
                    questionRecyclerView.apply {
                        setHasFixedSize(true)
                        setLayoutManager(layoutManager)
                    }
                    questionRecyclerView.adapter =
                        ReportQuestionListAdapter(answersList, QuestionReportType.UNKNOWN)
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
                    "fonts/JoshOpenSans-SemiBold.ttf"
                )
            )
            chart.setCenterTextSize(12F)
            chart.setExtraOffsets(0F, 0F, 0F, -15F)
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
            percentData.add(PieEntry(certificateExamReport.unanswered.toFloat(), 2))

            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                percentData.clear()
                percentData.add(PieEntry(certificateExamReport.unanswered.toFloat(), 0))
            }
            val dataSet = PieDataSet(percentData, "")
            val colorCorrect = Color.parseColor("#3DD2B5")
            val colorInCorrect = Color.parseColor("#F6595A")
            val colorInUnAnswered = ContextCompat.getColor(context, R.color.disabled)

            dataSet.colors = mutableListOf(colorCorrect, colorInCorrect, colorInUnAnswered)

            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                dataSet.color = ContextCompat.getColor(context, R.color.disabled)
            }

            dataSet.setDrawValues(false)
            dataSet.sliceSpace = 0f

            val data = PieData(dataSet)
            chart.data = data
            data.setDrawValues(false)
            //chart.highlightValue(0f, 0, false)
        }
    }

    private fun generateCenterSpannableText(percent: Float): SpannableString {
        val s0 = String.format("%.02f", percent).plus("%")
        val span = SpannableString("$s0 Correct")
        span.setSpan(RelativeSizeSpan(1.75f), 0, s0.length, 0)
        return span
    }

    
    fun onClickImgBtnLeft(){
        questionRecyclerView.scrollToPosition(0)
    }

    
    fun onClickImgBtnRight(){
        questionRecyclerView.adapter?.itemCount?.let { questionRecyclerView.scrollToPosition(it-1) }
    }

    
    fun onClickCorrectView() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.RIGHT))
    }

    
    fun onClickUnAnsweredView() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNANSWERED))
    }

    
    fun onClickInCorrectView() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.WRONG))
    }
}
