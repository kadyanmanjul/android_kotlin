package com.joshtalks.joshskills.ui.course_progress_new

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding

class CourseProgressActivityNew : AppCompatActivity() {

    lateinit var binding: CourseProgressActivityNewBinding
    lateinit var adapter: CourseProgressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.course_progress_activity_new)
        adapter = CourseProgressAdapter(this)

//        binding.progressRv.layoutManager = GridLayoutManager(this,7)
        binding.progressRv.adapter = adapter
    }
}