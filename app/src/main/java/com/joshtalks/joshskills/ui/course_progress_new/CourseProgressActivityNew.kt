package com.joshtalks.joshskills.ui.course_progress_new

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding

class CourseProgressActivityNew : AppCompatActivity() {

    lateinit var binding: CourseProgressActivityNewBinding
    lateinit var adapter: ProgressActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.course_progress_activity_new)
        adapter = ProgressActivityAdapter(this)
        binding.progressRv.adapter = adapter

        setupToolbar()
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.back_iv).setOnClickListener {
            onBackPressed()
        }
    }
}