package com.joshtalks.joshskills.certificate.report

import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.common.repository.server.certification_exam.QuestionReportType

class ReportOverView3Adapter(
    fragment: CExamReportFragment,
    private val certificateExamReport: CertificateExamReportModel,
    private val totalQuestions: List<CertificationQuestion>,
    private val reportType: QuestionReportType
)
    : FragmentStateAdapter(fragment){

    override fun getItemCount() = 1

    override fun createFragment(position: Int) = FragReportOverView3(certificateExamReport,totalQuestions,reportType)

}