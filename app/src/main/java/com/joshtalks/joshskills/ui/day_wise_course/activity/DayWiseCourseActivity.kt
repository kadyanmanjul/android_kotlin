package com.joshtalks.joshskills.ui.day_wise_course.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.day_wise_course.adapter.LessonPagerAdapter


const val CHAT_OBJECT: String = "CHAT_OBJECT"

class DayWiseCourseActivity : AppCompatActivity() {

    private lateinit var binding: DaywiseCourseActivityBinding

    companion object {
        fun startDayWiseCourseActivity(
            context: Activity,
            requestCode: Int,
            chatModel: ChatModel
        ) {
            val intent = Intent(context, DayWiseCourseActivity::class.java).apply {
                putExtra(CHAT_OBJECT, chatModel)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }
    }

    var chatModel: ChatModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.daywise_course_activity
        )

        chatModel = intent.getParcelableExtra(CHAT_OBJECT)
        if (chatModel == null)
            finish()

        val titleView: TextView = findViewById(R.id.text_message_title)
        val helpIv: ImageView = findViewById(R.id.iv_help)
        titleView.text = "Lesson 1"
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }

        val adapter = LessonPagerAdapter(chatModel!!, supportFragmentManager, this.lifecycle)
        binding.lessonViewpager.adapter = adapter

        TabLayoutMediator(
            binding.lessonTabLayout,
            binding.lessonViewpager,
            object : TabLayoutMediator.TabConfigurationStrategy {
                override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                    when (position) {
                        0 -> tab.text = getString(R.string.grammer)
                        1 -> tab.text = getString(R.string.vocabulary)
                        2 -> tab.text = getString(R.string.reading)
                        3 -> tab.text = getString(R.string.speaking)
                    }
                }
            }).attach()

    }
}