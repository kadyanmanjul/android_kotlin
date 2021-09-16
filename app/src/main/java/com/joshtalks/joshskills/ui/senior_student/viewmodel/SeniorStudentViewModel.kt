package com.joshtalks.joshskills.ui.senior_student.viewmodel

import android.app.Application
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentRvAdapter
import com.joshtalks.joshskills.ui.senior_student.repository.SeniorStudentRepository

class SeniorStudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SeniorStudentRepository()
    val seniorStudentAdapter: SeniorStudentRvAdapter = SeniorStudentRvAdapter()
    val seniorStudentBenefitAdapter: SeniorStudentRvAdapter = SeniorStudentRvAdapter()
    var seniorStudentHeading = ObservableField("")

    suspend fun fetchSeniorStudentData() {
        val data = repository.getSeniorStudentData()
        seniorStudentBenefitAdapter.setData(data.benefits ?: listOf())
        seniorStudentAdapter.setData(data.seniorStudent ?: listOf())
        seniorStudentHeading.set(data.heading ?: "")
    }
}