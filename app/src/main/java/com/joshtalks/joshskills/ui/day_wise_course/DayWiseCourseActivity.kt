package com.joshtalks.joshskills.ui.day_wise_course

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.LESSON_INTERVAL
import com.joshtalks.joshskills.core.LESSON__CHAT_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.DaywiseCourseActivityBinding
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.ui.chat.LESSON_REQUEST_CODE
import com.joshtalks.joshskills.ui.day_wise_course.unlock_next_class.ActivityUnlockNextClass
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL


class DayWiseCourseActivity : CoreJoshActivity(),
    CapsuleActivityCallback {

    private lateinit var tabs: ViewGroup
    private var lessonModel: LessonModel? = null
    private var lessonCompleted: Boolean = false
    lateinit var titleView: TextView
    private var courseId: Int? = null
    private lateinit var binding: DaywiseCourseActivityBinding
    var lessonId: Int = 0
    var lessonInterval: Int = -1
    var chatId: String = EMPTY
    var conversastionId: String? = null
    var isBatchChanged: Boolean = false


    private val viewModel: CapsuleViewModel by lazy {
        ViewModelProvider(this).get(CapsuleViewModel::class.java)
    }

    companion object {
        const val LAST_LESSON_STATUS = "last_lesson_status"
        private val LESSON_ID = "lesson_id"
        fun getDayWiseCourseActivityIntent(
            context: Context,
            lessonId: Int,
            courseId: String,
            interval: Int = -1,
            chatId: String = EMPTY
        ) = Intent(context, DayWiseCourseActivity::class.java).apply {
            putExtra(LESSON_ID, lessonId)
            putExtra(COURSE_ID, courseId)
            putExtra(LESSON_INTERVAL, interval)
            putExtra(LESSON__CHAT_ID, chatId)
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
            chatId = intent.getStringExtra(LESSON__CHAT_ID)
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

        lessonInterval = intent.getIntExtra(LESSON_INTERVAL, -1)
        chatId = intent.getStringExtra(LESSON__CHAT_ID)


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

            lessonModel = it.getOrNull(0)?.question?.lesson
            titleView.text =
                getString(R.string.lesson_no, it.getOrNull(0)?.question?.lesson?.lessonNo)

            setUpTablayout(it)


        })

        viewModel.lessonStatusLiveData.observe(this, {

            viewModel.updateQuestionLessonStatus(lessonId, it)
            if (it == LESSON_STATUS.CO) {
                lessonCompleted = true
            }
        })
    }

    private fun setUpTablayout(chatModellist: List<ChatModel>) {
        val sectionWiseChatList = ArrayList<ArrayList<ChatModel>>()
        val grammarQuestions: ArrayList<ChatModel> = ArrayList()
        val vocabularyQuestions: ArrayList<ChatModel> = ArrayList()
        val readingQuestions: ArrayList<ChatModel> = ArrayList()

        chatModellist.forEach {
            when (it.question?.chatType) {
                CHAT_TYPE.GR -> {
                    grammarQuestions.add(it)
                }
                CHAT_TYPE.VP -> {
                    vocabularyQuestions.add(it)
                }
                CHAT_TYPE.RP -> {
                    readingQuestions.add(it)
                }
                else -> {

                }
            }
        }
        sectionWiseChatList.add(grammarQuestions)
        sectionWiseChatList.add(vocabularyQuestions)
        sectionWiseChatList.add(readingQuestions)

        val adapter = LessonPagerAdapter(
            supportFragmentManager, this.lifecycle, chatModellist,
            courseId = courseId?.toString() ?: EMPTY,
            lessonId = lessonId
        )

        binding.lessonViewpager.adapter = adapter

        val tabs = binding.lessonTabLayout.getChildAt(0) as ViewGroup
        val layoutParam: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )



        TabLayoutMediator(
            binding.lessonTabLayout,
            binding.lessonViewpager,
            object : TabLayoutMediator.TabConfigurationStrategy {
                override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                    tab.setCustomView(R.layout.capsule_tab_layout_view)
                    when (position) {
                        0 -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setColor(tab)
                            }
                            tab.view.findViewById<TextView>(R.id.title_tv).text =
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.GRAMMAR_TITLE)

                        }
                        1 -> {
                            setUnselectedColor(tab)
                            tab.view.findViewById<TextView>(R.id.title_tv).text =
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.VOCABULARY_TITLE)
                        }
                        2 -> {
                            setUnselectedColor(tab)
                            tab.view.findViewById<TextView>(R.id.title_tv).text =
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.READING_TITLE)
                        }
                        3 -> {
                            setUnselectedColor(tab)
                            tab.view.findViewById<TextView>(R.id.title_tv).text =
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.SPEAKING_TITLE)
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
        for (i in 0 until tabs.childCount) {
            val tab = tabs.getChildAt(i)
            tab.layoutParams = layoutParam
            val layoutParams = tab.layoutParams as LinearLayout.LayoutParams

            layoutParams.weight = 0f
            layoutParams.marginEnd = Utils.dpToPx(2)
            layoutParams.marginStart = Utils.dpToPx(2)

            tab.layoutParams = layoutParams
            binding.lessonTabLayout.requestLayout()
        }
        var tabOpened = false
        var incompleteFound = false
        sectionWiseChatList.forEachIndexed outer@{ index, sectionChats ->
            incompleteFound = false
            sectionChats.forEach inner@{
                if (it.question?.status == QUESTION_STATUS.NA) {
                    if (tabOpened.not()) {
                        onNextTabCall(index)
                        tabOpened = true
                    }
                    incompleteFound = true
                    setTabCompletionStatus(tabs.getChildAt(index), false)
                    return@inner
                }
            }

            if (incompleteFound.not())
                setTabCompletionStatus(tabs.getChildAt(index), true)
            incompleteFound = false
        }
    }

    private fun setTabCompletionStatus(tab: View?, status: Boolean) {
        tab?.let {
            if (status) {
                it.findViewById<ImageView>(R.id.tab_iv).drawable.setTint(
                    ContextCompat.getColor(
                        this,
                        R.color.green_right_answer
                    )
                )
            } else {
                it.findViewById<ImageView>(R.id.tab_iv).drawable.setTint(
                    ContextCompat.getColor(
                        this,
                        R.color.grey
                    )
                )
            }
        }
    }

    private fun setUnselectedColor(tab: TabLayout.Tab?) {
        tab?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tab.view.background = ContextCompat.getDrawable(this, R.drawable.unselected_tab_bg)
                tab.view.findViewById<TextView>(R.id.title_tv)
                    ?.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setColor(tab: TabLayout.Tab?) {
        tab?.let {

            tab.view.findViewById<TextView>(R.id.title_tv)
                ?.setTextColor(ContextCompat.getColor(this, R.color.white))

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
            setResult(RESULT_OK, Intent().apply {
                putExtra(IS_BATCH_CHANGED, false)
                putExtra(LAST_LESSON_INTERVAL, lessonInterval)
                putExtra(LAST_LESSON_STATUS, lessonCompleted)
                putExtra(LESSON__CHAT_ID, chatId)
            })
            finish()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNextTabCall(tabNumber: Int) {
        try {
            binding.lessonViewpager.currentItem = tabNumber
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onQuestionStatusUpdate(status: QUESTION_STATUS, questionId: Int) {
        viewModel.updateQuestionStatus(status, questionId, courseId!!, lessonId)
    }

    override fun onContinueClick() {
        if (lessonModel != null) {
            conversastionId?.let { id ->
                startActivityForResult(
                    ActivityUnlockNextClass.getActivityUnlockNextClassIntent(
                        this,
                        id, lessonModel!!
                    ), LESSON_REQUEST_CODE
                )
            }
        }
    }

    override fun onSectionStatusUpdate(tabPosition: Int, status: Boolean) {
        println("tabPosition = [${tabPosition}], status = [${status}]")
        setTabCompletionStatus(tabs.getChildAt(tabPosition), status)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(IS_BATCH_CHANGED, isBatchChanged)
        resultIntent.putExtra(LAST_LESSON_INTERVAL, lessonInterval)
        resultIntent.putExtra(LAST_LESSON_STATUS, lessonCompleted)
        resultIntent.putExtra(LESSON__CHAT_ID, chatId)
        setResult(RESULT_OK, resultIntent)
        this@DayWiseCourseActivity.finish()
    }

}

