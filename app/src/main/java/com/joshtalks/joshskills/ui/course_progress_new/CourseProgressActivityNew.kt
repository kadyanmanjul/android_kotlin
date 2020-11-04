package com.joshtalks.joshskills.ui.course_progress_new

import android.content.DialogInterface
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.ui.day_wise_course.DayWiseCourseActivity

class CourseProgressActivityNew : AppCompatActivity(),
    CourseProgressAdapter.ProgressItemClickListener {

    lateinit var binding: CourseProgressActivityNewBinding
    lateinit var adapter: ProgressActivityAdapter

    private val viewModel: CourseOverviewViewModel by lazy {
        ViewModelProvider(this).get(CourseOverviewViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.course_progress_activity_new)

        setupToolbar()

        viewModel.progressLiveData.observe(this, {
            adapter = ProgressActivityAdapter(this, it, this)
            binding.progressRv.adapter = adapter

        })
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.back_iv).setOnClickListener {
            onBackPressed()
        }
    }

    override fun onProgressItemClick(item: CourseOverviewItem) {
        if (item.status == QUESTION_STATUS.AT.name) {
            startActivity(DayWiseCourseActivity.getDayWiseCourseActivityIntent(this, item.lessonId))
        } else
            showAlertMessage()
    }

    override fun onCertificateExamClick() {
        showAlertMessage()
    }

    private fun showAlertMessage() {
        val builder = AlertDialog.Builder(this)
            .setMessage("Please complete all previous lessons to unlock")
            .setPositiveButton("Okay") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .show()
    }
}