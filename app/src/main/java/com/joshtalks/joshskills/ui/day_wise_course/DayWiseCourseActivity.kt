package com.joshtalks.joshskills.ui.day_wise_course

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew

class DayWiseCourseActivity : CoreJoshActivity() {

    private var courseId: Int? = null
    private lateinit var binding: DaywiseCourseActivityBinding
    var lessonId: Int = 0
//    val questionList: ArrayList<Question> = ArrayList()

//    lateinit var chatList: ArrayList<ChatModel>

    private val viewModel: CapsuleViewModel by lazy {
        ViewModelProvider(this).get(CapsuleViewModel::class.java)
    }

    companion object {
        private val LESSON_ID = "lesson_id"
        fun getDayWiseCourseActivityIntent(
            context: Context,
            lessonId: Int
        ) = Intent(context, DayWiseCourseActivity::class.java).apply {
            putExtra(LESSON_ID, lessonId)
//            putExtra(CHAT_ITEMS, chatList)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (intent.hasExtra(LESSON_ID).not())
                finish()

            lessonId = intent.getIntExtra(LESSON_ID, 0)
            viewModel.syncQuestions(lessonId)
            viewModel.getQuestions(lessonId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.daywise_course_activity
        )

        if (intent.hasExtra(LESSON_ID).not())
            finish()

        lessonId = intent.getIntExtra(LESSON_ID, 0)
//        chatList = intent.getParcelableArrayListExtra(CHAT_ITEMS)!!

        val titleView: TextView = findViewById(R.id.text_message_title)
        val helpIv: ImageView = findViewById(R.id.iv_help)

        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }


        viewModel.syncQuestions(lessonId)
        viewModel.getQuestions(lessonId)

        viewModel.chatObservableLiveData.observe(this, {
            courseId = it.getOrNull(0)?.question?.course_id
            courseId?.let { courseId ->
                titleView.setOnClickListener {
                    startActivity(
                        CourseProgressActivityNew.getCourseProgressActivityNew(
                            this,
                            courseId
                        )
                    )
                }
            }
            titleView.text = it.getOrNull(0)?.question?.lesson?.lessonName
            val adapter = LessonPagerAdapter(it, supportFragmentManager, this.lifecycle)
            binding.lessonViewpager.adapter = adapter
            TabLayoutMediator(
                binding.lessonTabLayout,
                binding.lessonViewpager,
                object : TabLayoutMediator.TabConfigurationStrategy {
                    override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {

                        when (position) {
                            0 -> {
                                tab.text = getString(R.string.grammar)
                            }
                            1 -> {
                                tab.text = getString(R.string.vocabulary)
                            }
                            2 -> {
                                tab.text = getString(R.string.reading)
                            }
                            3 -> {
                                tab.text = getString(R.string.speaking)
                            }
                        }
                    }
                }).attach()
            val tabs = binding.lessonTabLayout.getChildAt(0) as ViewGroup


            val layoutParam: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )

            for (i in 0 until tabs.childCount) {
                val tab = tabs.getChildAt(i)
                tab.layoutParams = layoutParam
                val layoutParams = tab.layoutParams as LinearLayout.LayoutParams

                layoutParams.weight = 0f
                layoutParams.topMargin = 0
                layoutParams.marginStart = 0
                layoutParams.marginEnd = 0
//                binding.lessonTabLayout.layoutParams = layoutParams
                tab.layoutParams = layoutParams
                tab.setBackgroundColor(ContextCompat.getColor(this, R.color.quantum_pink))
                binding.lessonTabLayout.requestLayout()
            }

        })

    }
}
