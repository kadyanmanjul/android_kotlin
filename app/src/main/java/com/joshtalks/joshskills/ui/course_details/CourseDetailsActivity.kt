package com.joshtalks.joshskills.ui.course_details

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ActivityCourseDetailsBinding
import com.joshtalks.joshskills.repository.server.course_detail.*
import com.joshtalks.joshskills.ui.view_holders.BaseCell
import com.joshtalks.joshskills.ui.view_holders.SingleImageViewHolder

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_details)
        binding.lifecycleOwner = this
        binding.handler = this
        val testId = intent.getIntExtra(KEY_TEST_ID, 0)
        if (testId != 0) {
            getCourseDetails(testId)
        } else {
            finish()
        }

        addObserver()
    }

    private fun addObserver() {

        viewModel.courseDetailsLiveData.observe(this, Observer { list ->
            list.sortedBy { it.sequenceNumber }.forEach { card ->
                val cardViewHolder = getViewHolder(card)
                binding.placeHolderView.addView(cardViewHolder)
            }
        })

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
        })

    }

    private fun getCourseDetails(testId: Int) {
        viewModel.fetchCourseDetails(testId)
    }

    private fun getViewHolder(card: Card): BaseCell = when (card.cardType) {
        CardType.COURSE_OVERVIEW -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                CourseOverviewData::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.TEACHER_DETAILS -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                TeacherDetails::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.LONG_DESCRIPTION -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                LongDescription::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.SYLLABUS -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                SyllabusData::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.GUIDELINES -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                Guidelines::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.DEMO_LESSON -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                DemoLesson::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.REVIEWS -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                Reviews::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.LOCATION_STATS -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                LocationStats::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.STUDENT_FEEDBACK -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                StudentFeedback::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.FAQ -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                FAQData::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.ABOUT_JOSH -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                AboutJosh::class.java
            )
            // TODO - return ViewHolder(data)
        }
        CardType.OTHER_INFO -> {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                card.data.toString(),
                SingleImageData::class.java
            )
            SingleImageViewHolder(data.imgUrl)
        }
    } as BaseCell


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
