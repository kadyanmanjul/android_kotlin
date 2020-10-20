package com.joshtalks.joshskills.ui.day_wise_course.activity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityDailyLessonsBinding
import com.joshtalks.joshskills.ui.day_wise_course.adapter.LessonsAdapter
import com.joshtalks.joshskills.ui.day_wise_course.lesson.LessonsViewModel

class DailyLessonsActivity : CoreJoshActivity() {

    lateinit var binding: ActivityDailyLessonsBinding

    lateinit var adapter: LessonsAdapter
    private val viewModel: LessonsViewModel by lazy {
        ViewModelProvider(this).get(LessonsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_daily_lessons)

        adapter = LessonsAdapter(this)
        binding.lessonsRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding.lessonsRv.adapter = adapter

        viewModel.getLessons()?.observe(this, { lessons ->
            adapter.submitList(lessons)
            binding.lessonsRv.scrollToPosition(0)

        })

    }
}