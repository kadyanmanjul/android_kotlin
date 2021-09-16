package com.joshtalks.joshskills.ui.senior_student.common

import android.os.Build
import android.text.Html
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentRvAdapter



    @BindingAdapter("android:seniorStudentText")
    fun setSeniorStudentText(textView : AppCompatTextView, html: String) {
        textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    @BindingAdapter("android:seniorStudentAdapter")
    fun setSeniorStudentAdapter(recyclerView : RecyclerView, adapter: SeniorStudentRvAdapter) {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter
    }