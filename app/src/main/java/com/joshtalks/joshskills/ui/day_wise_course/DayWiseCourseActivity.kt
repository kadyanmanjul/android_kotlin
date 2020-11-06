package com.joshtalks.joshskills.ui.day_wise_course

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew


class DayWiseCourseActivity : CoreJoshActivity(),
    OnFragmentNavigationListener {

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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    setColor(tab)
                                }
                                tab.text = getString(R.string.grammar)
                            }
                            1 -> {
                                setUnselectedColor(tab)
                                tab.text = getString(R.string.vocabulary)
                            }
                            2 -> {
                                setUnselectedColor(tab)
                                tab.text = getString(R.string.reading)
                            }
                            3 -> {
                                setUnselectedColor(tab)
                                tab.text = getString(R.string.speaking)
                            }
                        }
                    }
                }).attach()

            binding.lessonTabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setColor(tab)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setColor(tab)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    setUnselectedColor(tab)
                }
            })

        })

    }

    private fun setUnselectedColor(tab: TabLayout.Tab?) {
        tab?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tab.view.background = ContextCompat.getDrawable(this, R.drawable.unselected_tab_bg)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setColor(tab: TabLayout.Tab?) {
        tab?.let {

            when (tab.position) {
                0 -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.capsule_selection_tab)
                }
                1 -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.vocabulary_tab_bg)
                }
                2 -> {
                    tab.view.background = ContextCompat.getDrawable(this, R.drawable.reading_tab_bg)
                }
                3 -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.speaking_tab_bg)

                }
            }
        }
    }

    override fun onNextTabCall(view: View?) {
        try {
            binding.lessonViewpager.currentItem = ++binding.lessonViewpager.currentItem
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}

interface OnFragmentNavigationListener {
    fun onNextTabCall(view: View?)
}
