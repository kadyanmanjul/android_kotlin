package com.joshtalks.joshskills.premium.ui.certification_exam.report

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.premium.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.premium.repository.server.certification_exam.CertificationQuestion

class CExamReportAdapter(
    fm: FragmentActivity,
    var list: List<CertificateExamReportModel>,
    var questions: List<CertificationQuestion>?
) :
    FragmentStateAdapter(fm) {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return CExamReportFragment.newInstance(list[position], questions)
    }

}
