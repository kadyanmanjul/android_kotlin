package com.joshtalks.joshskills.certificate.report

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.certificate.R
import com.joshtalks.joshskills.certificate.constants.BTN_CHANGED_TEXT
import com.joshtalks.joshskills.certificate.constants.EXAM_TYPE_ADVANCED
import com.joshtalks.joshskills.certificate.constants.EXAM_TYPE_BEGINNER
import com.joshtalks.joshskills.certificate.constants.EXAM_TYPE_INTERMEDIATE
import com.joshtalks.joshskills.certificate.databinding.FragmentFragReportOverView1Binding
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.DownloadFileEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import java.text.DecimalFormat

class FragReportOverView1(private val certificateExamReport: CertificateExamReportModel, private val examType:String?) : Fragment() {

    private lateinit var binding : FragmentFragReportOverView1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_frag_report_over_view1,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        certificateExamReport.run {
            with(binding){
                headerTv.text = heading
                resultInfo.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (isExamPass) { //change for test
                    groupCertificateDownload.visibility = View.VISIBLE
                    checkExamDetails.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary_500
                        )
                    )
                    checkExamDetails.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.pure_white
                        )
                    )

                    checkExamDetails.strokeColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary_500
                        )
                    )

                    checkExamDetails.strokeWidth = 2

                    checkExamDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)

                    if (certificateURL.isNullOrEmpty()) {
                        btnDownloadCertificate.visibility = View.GONE
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
                        btnDownloadCertificate.text = BTN_CHANGED_TEXT
                        checkExamDetails.visibility = View.VISIBLE
                    }else{
                        checkExamDetails.visibility = View.GONE
                    }
                }

                scoreTv.text = getScoreText(score, maxScore)
            }

        }

        binding.btnDownloadCertificate.setOnClickListener {
            if (Utils.isInternetAvailable()){
                RxBus2.publish(
                    DownloadFileEventBus(
                        id = certificateExamReport.reportId,
                        url = certificateExamReport.certificateURL
                    )
                )
            }else{
                showToast("No Internet Available")
            }
        }

        binding.checkExamDetails.setOnClickListener {
            RxBus2.publish(EmptyEventBus())
        }
    }

    private fun getScoreText(score: Double, maxScore: Int): SpannableString? {
        val string1 = getString(R.string.your_score)
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
}