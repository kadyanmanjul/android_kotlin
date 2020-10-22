package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityDailyLessonsBinding
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.day_wise_course.DayWiseCourseActivity

class DailyLessonsActivity : CoreJoshActivity() {

    private lateinit var courseId: String
    lateinit var binding: ActivityDailyLessonsBinding

    lateinit var adapter: LessonsAdapter
    private val viewModel: LessonsViewModel by lazy {
        ViewModelProvider(this).get(LessonsViewModel::class.java)
    }

    companion object {
        const val COURSE_ID = "COURSE_ID"
        fun startDailyLessonsActivity(context: Context, courseId: String) =
            Intent(context, DailyLessonsActivity::class.java)
                .apply { this.putExtra(COURSE_ID, courseId) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(COURSE_ID))
            courseId = intent.getStringExtra(COURSE_ID)!!
        else
            finish()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_daily_lessons)

        adapter = LessonsAdapter(this, this::onItemClick)
        binding.lessonsRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding.lessonsRv.adapter = adapter

        viewModel.syncLessonsWithServer(courseId)

        viewModel.getLessons()?.observe(this, { lessons ->
            adapter.submitList(lessons)
            binding.lessonsRv.scrollToPosition(0)

        })
    }

    fun onItemClick(lessonModel: LessonModel) {
        startActivity(
            DayWiseCourseActivity.getDayWiseCourseActivityIntent(
                this,
                "${lessonModel.id}",
                lessonModel.lessonName
            )
        )
    }
}