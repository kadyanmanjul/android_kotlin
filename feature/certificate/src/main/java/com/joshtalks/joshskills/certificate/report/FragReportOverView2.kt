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
import com.joshtalks.joshskills.certificate.databinding.FragmentFragReportOverView2Binding
import com.joshtalks.joshskills.certificate.report.vh.ReportQuestionListAdapter
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

class FragReportOverView2(private val certificateExamReport: CertificateExamReportModel,
                          private val totalQuestions: List<CertificationQuestion>) : Fragment() {

    private lateinit var binding: FragmentFragReportOverView2Binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_frag_report_over_view2,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        certificateExamReport.run {
            binding.tvCorrect.text = correct.toString()
            binding.tvIncorrect.text = wrong.toString()
            binding.tvUnanswered.text = unanswered.toString()
            setClickListeners()
            setChart()
            setQuestionRV()
        }
    }

    private fun setClickListeners() {

        with(binding){
            imgBtnLeft.setOnClickListener{
                recyclerView.scrollToPosition(0)
            }
            imgBtnRight.setOnClickListener {
                recyclerView.adapter?.itemCount?.let { recyclerView.scrollToPosition(it-1) }
            }
            llCorrect.setOnClickListener {
                RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.RIGHT))
            }
            llIncorrect.setOnClickListener {
                RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.WRONG))
            }
            llUnanswerd.setOnClickListener {
                RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNANSWERED))
            }
        }
    }

    private fun setQuestionRV() {
        CoroutineScope(Dispatchers.IO).launch {
            if (binding.recyclerView.adapter == null) {
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
                    with(binding.recyclerView){
                        addItemDecoration(
                            GridSpacingQuestionsDecoration(6, Utils.dpToPx(requireContext(), 6f), true)
                        )
                        setHasFixedSize(true)
                        val layoutManager = GridLayoutManager(requireContext(), 6, RecyclerView.HORIZONTAL,false)
                        setLayoutManager(layoutManager)
                        adapter = ReportQuestionListAdapter(answersList, QuestionReportType.UNKNOWN)
                    }
                }
            }
        }
    }

    private fun getAnswerStatus(certificationQuestion: CertificationQuestion): Boolean? {
        return certificateExamReport.answers?.find { it.question == certificationQuestion.questionId }?.isAnswerCorrect
    }

    private fun setChart() {
        CoroutineScope(Dispatchers.Main).launch {
            with(binding.chart){
                centerText = generateCenterSpannableText(certificateExamReport.percent)
                setCenterTextColor(Color.parseColor("#38B099"))
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
                setHoleColor(Color.WHITE)
                setTransparentCircleColor(Color.WHITE)
                animateY(500, Easing.EaseInOutQuad)
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                setDrawEntryLabels(false)
                description.isEnabled = false
            }

            val legend: Legend = binding.chart.legend

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
            val colorInUnAnswered = ContextCompat.getColor(requireContext(), R.color.disabled)

            dataSet.colors = mutableListOf(colorCorrect, colorInCorrect, colorInUnAnswered)

            if (certificateExamReport.correct == 0 && certificateExamReport.wrong == 0) {
                dataSet.color = ContextCompat.getColor(requireContext(), R.color.disabled)
            }

            dataSet.setDrawValues(false)
            dataSet.sliceSpace = 0f

            val data = PieData(dataSet)
            binding.chart.data = data
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

}