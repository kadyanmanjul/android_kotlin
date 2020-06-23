package com.joshtalks.joshskills.ui.course_details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R

class CourseDetailsActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_details)
        val testId = intent.getIntExtra(KEY_TEST_ID, 0)
        if (testId != 0) {
            getCourseDetails(testId)
        } else {
            finish()
        }
    }

    private fun getCourseDetails(testId: Int) {
        viewModel.fetchCourseDetails(testId)
    }


    companion object {
        const val KEY_TEST_ID = "test-id"

        fun startCourseDetailsActivity(activity: Activity, testId: Int) {
            Intent(activity, CourseDetailsActivity::class.java).apply {
                putExtra(KEY_TEST_ID, testId)
            }.run {
                activity.startActivity(this)
            }
        }
    }
}
