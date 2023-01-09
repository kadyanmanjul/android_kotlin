package com.joshtalks.joshskills.certificate.report

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion

class ReportOverviewAdapter(fragment: CExamReportFragment,
                            private val certificateExamReport: CertificateExamReportModel,
                            private val examtype:String?,
                            private val questionList: List<CertificationQuestion>
): FragmentStateAdapter(fragment)
{
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> FragReportOverView1(certificateExamReport,examtype)
        1 -> FragReportOverView2(certificateExamReport,questionList)
        else -> throw IllegalStateException("Invalid adapter position")
    }
}