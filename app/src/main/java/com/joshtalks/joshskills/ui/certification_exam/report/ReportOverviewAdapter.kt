package com.joshtalks.joshskills.ui.certification_exam.report

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion

class ReportOverviewAdapter(fragment: CExamReportFragment,
                            val certificateExamReport: CertificateExamReportModel,
                            val examtype:String?,
                            val questionList: List<CertificationQuestion>
                            ): FragmentStateAdapter(fragment)
{
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> FragReportOverView1(certificateExamReport,examtype)
        1 -> FragReportOverView2(certificateExamReport,questionList)
        else -> throw IllegalStateException("Invalid adapter position")
    }
}