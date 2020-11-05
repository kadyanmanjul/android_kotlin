package com.joshtalks.joshskills.ui.certification_exam.report

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentCexamReportBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.ui.certification_exam.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.ui.certification_exam.report.vh.ReportOverviewView1
import com.joshtalks.joshskills.ui.certification_exam.report.vh.ReportOverviewView2
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


class CExamReportFragment : Fragment() {

    companion object {
        private const val ARG_EXAM_REPORT = "exam_report"

        @JvmStatic
        fun newInstance(
            obj: CertificateExamReportModel,
            questionList: List<CertificationQuestion>?
        ) =
            CExamReportFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EXAM_REPORT, obj)
                    putParcelableArrayList(
                        CERTIFICATION_EXAM_QUESTION,
                        ArrayList(questionList ?: emptyList())
                    )
                }
            }
    }

    private lateinit var binding: FragmentCexamReportBinding
    private var certificateExamReport: CertificateExamReportModel? = null
    private var questionList: List<CertificationQuestion> = emptyList()
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            certificateExamReport = it.getParcelable(ARG_EXAM_REPORT)
            questionList =
                it.getParcelableArrayList(CERTIFICATION_EXAM_QUESTION)
                    ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_cexam_report, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        certificateExamReport?.run {
            binding.chatRv.addView(ReportOverviewView1(this))
            binding.chatRv.addView(ReportOverviewView2(this, questionList))
            updateRvScrolling(true)
        }

    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(EmptyEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateRvScrolling(false)
                    binding.chatRv.smoothScrollToPosition(1)
                }, {
                    it.printStackTrace()
                })
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateRvScrolling(flag: Boolean) {
        binding.chatRv.setOnTouchListener({ v, event -> flag })
    }


}