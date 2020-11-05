package com.joshtalks.joshskills.ui.certification_exam.report

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel

class CExamReportAdapter(fm: FragmentActivity, var list: List<CertificateExamReportModel>) :
    FragmentStateAdapter(fm) {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return CExamReportFragment.newInstance(list[position])
    }

}
