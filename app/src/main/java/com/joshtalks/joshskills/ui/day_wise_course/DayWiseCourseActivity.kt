package com.joshtalks.joshskills.ui.day_wise_course

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.repository.local.entity.ChatModel

class DayWiseCourseActivity : AppCompatActivity() {

    private lateinit var binding: DaywiseCourseActivityBinding
    lateinit var lessonId: String
//    val questionList: ArrayList<Question> = ArrayList()

    lateinit var chatList: ArrayList<ChatModel>

    private val viewModel: CapsuleViewModel by lazy {
        ViewModelProvider(this).get(CapsuleViewModel::class.java)
    }

    companion object {
        private val LESSON_ID = "lesson_id"
        private val CHAT_ITEMS = "chat_items"
        fun getDayWiseCourseActivityIntent(
            context: Context,
            lessonId: String,
            chatList: ArrayList<ChatModel>
        ) = Intent(context, DayWiseCourseActivity::class.java).apply {
            putExtra(LESSON_ID, lessonId)
            putExtra(CHAT_ITEMS, chatList)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.daywise_course_activity
        )

        if (intent.hasExtra(CHAT_ITEMS).not())
            finish()

        lessonId = intent.getStringExtra(LESSON_ID)
        chatList = intent.getParcelableArrayListExtra(CHAT_ITEMS)!!

        val titleView: TextView = findViewById(R.id.text_message_title)
        val helpIv: ImageView = findViewById(R.id.iv_help)
        titleView.text = chatList.get(0).question?.lesson?.lessonName
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }

        val adapter = LessonPagerAdapter(chatList, supportFragmentManager, this.lifecycle)
        binding.lessonViewpager.adapter = adapter
        TabLayoutMediator(
            binding.lessonTabLayout,
            binding.lessonViewpager,
            object : TabLayoutMediator.TabConfigurationStrategy {
                override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                    when (position) {
                        0 -> tab.text = getString(R.string.grammar)
                        1 -> tab.text = getString(R.string.vocabulary)
                        2 -> tab.text = getString(R.string.reading)
                        3 -> tab.text = getString(R.string.speaking)
                    }
                }
            }).attach()

//        viewModel.syncQuestions(lessonId)
//        viewModel.getQuestions(lessonId)

    }
}