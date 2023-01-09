package com.joshtalks.joshskills.certificate.report

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.joshtalks.joshskills.certificate.R
import com.joshtalks.joshskills.certificate.databinding.FragmentFragReportOverView3Binding
import com.joshtalks.joshskills.certificate.report.vh.ReportQuestionListAdapter
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.decorator.GridSpacingQuestionsDecoration
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenReportQTypeEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.common.repository.server.certification_exam.QuestionReportType
import com.joshtalks.joshskills.common.repository.server.certification_exam.UserSelectedAnswer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FragReportOverView3(private val certificateExamReport: CertificateExamReportModel,
                          private val totalQuestions: List<CertificationQuestion>,
                          private val reportType: QuestionReportType
) : Fragment() {

    private lateinit var binding: FragmentFragReportOverView3Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_frag_report_over_view3,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        certificateExamReport.run {
            binding.tvCorrect.text = correct.toString()
            binding.tvIncorrect.text = wrong.toString()
            binding.tvUnanswered.text = unanswered.toString()
            when (reportType) {
                QuestionReportType.RIGHT -> {
                    binding.llCorrect.visibility = View.VISIBLE
                }
                QuestionReportType.WRONG -> {
                    binding.llIncorrect.visibility = View.VISIBLE
                }
                QuestionReportType.UNANSWERED -> {
                    binding.llUnanswerd.visibility = View.VISIBLE
                }
                else -> {}
            }
            setChart()
            setQuestionRV()
        }

        binding.rootView.setOnClickListener {
            RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNKNOWN))
        }
    }

    private fun setQuestionRV() {
        if ( binding.recyclerView.adapter == null) {
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
            binding.recyclerView.apply {
                addItemDecoration(GridSpacingQuestionsDecoration(6, Utils.dpToPx(context, 6f), true))
                setHasFixedSize(true)
                layoutManager = GridLayoutManager(context, 6, RecyclerView.HORIZONTAL, false)
                adapter = ReportQuestionListAdapter(answersList, reportType)
            }
        }
    }

    private fun getAnswerStatus(certificationQuestion: CertificationQuestion): Boolean? {
        return certificateExamReport.answers?.find { it.question == certificationQuestion.questionId }?.isAnswerCorrect
    }

    private fun setChart() {
        CoroutineScope(Dispatchers.Main).launch {
            with(binding.chart){
                setCenterTextTypeface(
                    Typeface.createFromAsset(
                        requireContext().assets,
                        "fonts/JoshOpenSans-SemiBold.ttf"
                    )
                )
                setCenterTextSize(12F)
                setExtraOffsets(0F, 0F, 0F, -15F)
                holeRadius = 58f
                transparentCircleRadius = 58F
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                setTransparentCircleColor(Color.TRANSPARENT)
                animateY(0, Easing.EaseInOutQuad)
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                setDrawEntryLabels(false)
                description.isEnabled = false
            }

            val legend: Legend =  binding.chart.legend
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
            var colorCorrect: Int = ContextCompat.getColor(requireContext(), R.color.transparent)
            var colorInCorrect: Int = ContextCompat.getColor(requireContext(), R.color.transparent)
            var colorInUnAnswered: Int = ContextCompat.getColor(requireContext(), R.color.transparent)

            val textColor: Int
            val percentText: String
            when {
                QuestionReportType.WRONG == reportType -> {
                    colorInCorrect = Color.parseColor("#F6595A")
                    textColor = colorInCorrect
                    percentText = EMPTY + getPercent(certificateExamReport.wrong).plus(" Incorrect")
                }
                QuestionReportType.UNANSWERED == reportType -> {
                    colorInUnAnswered = ContextCompat.getColor(requireContext(), R.color.disabled)
                    textColor = colorInUnAnswered
                    percentText =
                        EMPTY + getPercent(certificateExamReport.unanswered).plus(" Unanswered")

                }
                else -> {
                    colorCorrect = Color.parseColor("#3DD2B5")
                    textColor = colorCorrect
                    percentText = EMPTY + getPercent(certificateExamReport.correct).plus(" Correct")
                }
            }
            binding.chart.setCenterTextColor(textColor)
            dataSet.colors = mutableListOf(colorCorrect, colorInCorrect, colorInUnAnswered)
            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                dataSet.color = ContextCompat.getColor(requireContext(), R.color.disabled)
            }
            binding.chart.centerText = generateCenterSpannableText(percentText)

            dataSet.setDrawValues(false)
            dataSet.sliceSpace = 0f

            val data = PieData(dataSet)
            binding.chart.data = data
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

    private fun getPercent(per: Int): String {
        val percent =
            (per.toFloat() * 100) / (certificateExamReport.correct + certificateExamReport.wrong + certificateExamReport.unanswered)
        return String.format("%.02f", percent).plus("%")
    }

}