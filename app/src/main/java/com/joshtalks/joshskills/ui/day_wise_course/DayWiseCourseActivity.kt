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
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.LESSON_INTERVAL
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.ui.chat.LESSON_REQUEST_CODE
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew
import com.joshtalks.joshskills.ui.day_wise_course.unlock_next_class.ActivityUnlockNextClass
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL


class DayWiseCourseActivity : CoreJoshActivity(),
    CapsuleActivityCallback {

    lateinit var titleView: TextView
    private var courseId: Int? = null
    private lateinit var binding: DaywiseCourseActivityBinding
    var lessonId: Int = 0
    var lessonInterval: Int = -1
    var conversastionId: String? = null
    var isBatchChanged: Boolean = false

    private val viewModel: CapsuleViewModel by lazy {
        ViewModelProvider(this).get(CapsuleViewModel::class.java)
    }

    companion object {
        private val LESSON_ID = "lesson_id"
        fun getDayWiseCourseActivityIntent(
            context: Context,
            lessonId: Int,
            courseId: String,
            interval: Int = -1
        ) = Intent(context, DayWiseCourseActivity::class.java).apply {
            putExtra(LESSON_ID, lessonId)
            putExtra(COURSE_ID, courseId)
            putExtra(LESSON_INTERVAL, interval)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (intent.hasExtra(LESSON_ID).not())
                finish()

            lessonId = intent.getIntExtra(LESSON_ID, 0)
            lessonInterval = intent.getIntExtra(LESSON_INTERVAL, -1)
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


        lessonInterval = intent.getIntExtra(LESSON_INTERVAL, -1)

        titleView = findViewById(R.id.text_message_title)
        val helpIv: ImageView = findViewById(R.id.iv_help)

        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }


        viewModel.syncQuestions(lessonId)
        viewModel.getQuestions(lessonId)

        setObservers()

    }

    private fun setObservers() {
        viewModel.chatObservableLiveData.observe(this, {
            courseId = it.getOrNull(0)?.question?.course_id
            conversastionId = it.getOrNull(0)?.conversationId

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

            val adapter = LessonPagerAdapter(
                supportFragmentManager, this.lifecycle, it,
                courseId = courseId?.toString() ?: EMPTY,
                lessonId = lessonId
            )
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

        viewModel.lessonStatusLiveData.observe(this, {
            if (it == LESSON_STATUS.CO.name) {
                conversastionId?.let { id ->
                    startActivityForResult(
                        ActivityUnlockNextClass.getActivityUnlockNextClassIntent(
                            this,
                            id
                        ), LESSON_REQUEST_CODE
                    )
                }
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LESSON_REQUEST_CODE && resultCode == RESULT_OK && data?.hasExtra(
                IS_BATCH_CHANGED
            ) == true
        ) {
            setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, true) })
            finish()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNextTabCall(view: View?) {
        try {
            binding.lessonViewpager.currentItem = ++binding.lessonViewpager.currentItem
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onQuestionStatusUpdate(status: String, questionId: Int) {
        viewModel.updateQuestionStatus(status, questionId, courseId!!, lessonId)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(IS_BATCH_CHANGED, isBatchChanged)
        resultIntent.putExtra(LAST_LESSON_INTERVAL, lessonInterval)

        setResult(RESULT_OK, resultIntent)
        this@DayWiseCourseActivity.finish()
    }
}

interface CapsuleActivityCallback {
    fun onNextTabCall(view: View?)
    fun onQuestionStatusUpdate(status: String, questionId: Int)
}
