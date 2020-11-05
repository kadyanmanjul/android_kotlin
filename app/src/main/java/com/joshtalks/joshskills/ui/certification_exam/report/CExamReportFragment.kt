package com.joshtalks.joshskills.ui.certification_exam.report

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentCexamReportBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel


class CExamReportFragment : Fragment() {

    companion object {
        private const val ARG_EXAM_REPORT = "exam_report"

        @JvmStatic
        fun newInstance(obj: CertificateExamReportModel) =
            CExamReportFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EXAM_REPORT, obj)
                }
            }
    }

    private lateinit var binding: FragmentCexamReportBinding
    private var certificateExamReport: CertificateExamReportModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            certificateExamReport = it.getParcelable(ARG_EXAM_REPORT)
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
            binding.headerTv.text = heading
            binding.resultInfo.text = text
            if (true) {
                binding.groupCertificateDownload.visibility = View.VISIBLE
                binding.checkExamDetails.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorAccent
                    )
                )
                binding.checkExamDetails.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            }
            binding.groupCertificateDownload.visibility = View.VISIBLE
            binding.checkExamDetails.visibility = View.VISIBLE

            binding.scoreTv.text = "Dwwdw"
        }

    }

    private fun getScoreText(score: Double, maxScore: Int): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder()

        val string1 = getString(R.string.your_score)
        val span1 = SpannableString(string1)
        span1.setSpan(AbsoluteSizeSpan(R.dimen._14ssp), 0, string1.length, 0)
        spannableStringBuilder.append(span1)

        val string2 = score.toString().plus(maxScore.toString())

        val span2 = SpannableString(string2)
        span2.setSpan(AbsoluteSizeSpan(R.dimen._20ssp), 0, string2.length, 0)
        spannableStringBuilder.append(span2)

        return spannableStringBuilder
    }
}