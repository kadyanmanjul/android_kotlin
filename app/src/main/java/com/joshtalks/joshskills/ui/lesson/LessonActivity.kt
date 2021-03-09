package com.joshtalks.joshskills.ui.lesson

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.LessonActivityBinding
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL

class LessonActivity : CoreJoshActivity(), LessonActivityListener {

    private lateinit var binding: LessonActivityBinding

    //lateinit var lesson: LessonModel
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }

    lateinit var titleView: TextView
    private lateinit var tabs: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.lesson_activity
        )

        val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0

        titleView = findViewById(R.id.text_message_title)

        setObservers()
        viewModel.getLesson(lessonId)
        viewModel.getQuestions(lessonId)

        val helpIv: ImageView = findViewById(R.id.iv_help)
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
    }

    private fun setObservers() {

//        viewModel.lessonLiveData.observe(this, {
//            setUpTabLayout()
//            setTabCompletionStatus()
//        })

        viewModel.lessonQuestionsLiveData.observe(this, {
            binding.progressView.visibility = View.GONE
            viewModel.lessonLiveData.value?.let {
                titleView.text =
                    getString(R.string.lesson_no, it.lessonNo)
            }
            setUpTabLayout()
            setTabCompletionStatus()
        })

        viewModel.updatedLessonResponseLiveData.observe(this, {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList?.get(0))
                playSnackbarSound(this)
            }
            /*if (it.awardMentorList.isNullOrEmpty().not()) {
                //TODO add when awards functionality is over
                //ShowAwardFragment.showDialog(supportFragmentManager,it.awardMentorList!!)
            }
            if (it.outranked!!) {
                it.outrankedData?.let {
                    showLeaderboardAchievement(
                        it,
                        lessonInterval,
                        chatId,
                        lessonModel?.lessonNo ?: 0
                    )
                }
            }*/
        })

        viewModel.pointsSnackBarText.observe(
            this,
            {
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
                }
            })

    }

    override fun onNextTabCall(currentTabNumber: Int) {
        try {
            viewModel.lessonLiveData.value?.let { lesson ->
                val lessonCompleted = lesson.grammarStatus == LESSON_STATUS.CO &&
                        lesson.vocabStatus == LESSON_STATUS.CO &&
                        lesson.readingStatus == LESSON_STATUS.CO &&
                        lesson.speakingStatus == LESSON_STATUS.CO

                lesson.status = LESSON_STATUS.CO

                if (lessonCompleted) {
                    openLessonCompleteScreen(lesson)
                } else
                    openIncompleteTab(currentTabNumber)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onQuestionStatusUpdate(
        status: QUESTION_STATUS,
        questionId: String?,
        isVideoPercentComplete: Boolean,
        quizCorrectQuestionIds: ArrayList<Int>
    ) {
        viewModel.updateQuestionStatus(
            status,
            questionId,
            isVideoPercentComplete,
            quizCorrectQuestionIds
        )
        setTabCompletionStatus()
    }

    override fun onSectionStatusUpdate(tabPosition: Int, isSectionCompleted: Boolean) {
        AppObjectController.uiHandler.post {


            viewModel.lessonLiveData.value?.let { lesson ->
                val status = if (isSectionCompleted) LESSON_STATUS.CO else LESSON_STATUS.NO
                when (tabPosition) {
                    0 -> lesson.grammarStatus = status
                    1 -> lesson.vocabStatus = status
                    2 -> lesson.readingStatus = status
                    3 -> lesson.speakingStatus = status
                }
                viewModel.updateSectionStatus(lesson.id, status, tabPosition)
            }
            setTabCompletionStatus()
        }
    }


    private fun setUpTabLayout() {
        val adapter = LessonPagerAdapter(
            supportFragmentManager,
            this.lifecycle
        )

        binding.lessonViewpager.adapter = adapter
        binding.lessonViewpager.requestTransparentRegion(binding.lessonViewpager)

        tabs = binding.lessonTabLayout.getChildAt(0) as ViewGroup
        for (i in 0 until tabs.childCount) {
            val tab = tabs.getChildAt(i)
            val layoutParams = tab.layoutParams as LinearLayout.LayoutParams
            layoutParams.weight = 0f
            layoutParams.marginEnd = Utils.dpToPx(2)
            layoutParams.marginStart = Utils.dpToPx(2)
        }
        binding.lessonTabLayout.requestLayout()

        TabLayoutMediator(
            binding.lessonTabLayout,
            binding.lessonViewpager
        ) { tab, position ->
            tab.setCustomView(R.layout.capsule_tab_layout_view)
            when (position) {
                0 -> {
                    setSelectedColor(tab)
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
        }.attach()

        binding.lessonTabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                setSelectedColor(tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                setSelectedColor(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                setUnselectedColor(tab)
            }
        })

        Handler().postDelayed({
            openIncompleteTab(3)
        }, 50)
    }

    private fun openIncompleteTab(currentTabNumber: Int) {
        var nextTabIndex = currentTabNumber + 1
        while (nextTabIndex != currentTabNumber) {
            if (nextTabIndex == 4) {
                nextTabIndex = 0
            } else {
                viewModel.lessonLiveData.value?.let { lesson ->
                    when (nextTabIndex) {
                        0 ->
                            if (lesson.grammarStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = 0
                                return
                            } else {
                                nextTabIndex++
                            }
                        1 ->
                            if (lesson.vocabStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = 1
                                return
                            } else {
                                nextTabIndex++
                            }
                        2 ->
                            if (lesson.readingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = 2
                                return
                            } else {
                                nextTabIndex++
                            }
                        3 ->
                            if (lesson.speakingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = 3
                                return
                            } else {
                                nextTabIndex++
                            }
                        else -> {
                            binding.lessonViewpager.currentItem = 3
                            return
                        }
                    }
                }
            }
        }
    }

    private fun setTabCompletionStatus() {
        viewModel.lessonLiveData.value?.let { lesson ->
            setTabCompletionStatus(
                tabs.getChildAt(0),
                lesson.grammarStatus == LESSON_STATUS.CO
            )
            setTabCompletionStatus(
                tabs.getChildAt(1),
                lesson.vocabStatus == LESSON_STATUS.CO
            )
            setTabCompletionStatus(
                tabs.getChildAt(2),
                lesson.readingStatus == LESSON_STATUS.CO
            )
            setTabCompletionStatus(
                tabs.getChildAt(3),
                lesson.speakingStatus == LESSON_STATUS.CO
            )
        }
    }

    private fun setTabCompletionStatus(tab: View?, isSectionCompleted: Boolean) {
        tab?.let {
            if (isSectionCompleted) {
                it.findViewById<ImageView>(R.id.tab_iv).visibility = View.VISIBLE
            } else {
                it.findViewById<ImageView>(R.id.tab_iv).visibility = View.GONE
            }
        }
    }

    private fun setSelectedColor(tab: TabLayout.Tab?) {
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

    private fun setUnselectedColor(tab: TabLayout.Tab?) {
        tab?.let {
            tab.view.background = ContextCompat.getDrawable(this, R.drawable.unselected_tab_bg)
            tab.view.findViewById<TextView>(R.id.title_tv)
                ?.setTextColor(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun openLessonCompleteScreen(lesson: LessonModel) {
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data!!.hasExtra(IS_BATCH_CHANGED) == true) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(IS_BATCH_CHANGED, false)
                    putExtra(LAST_LESSON_INTERVAL, lesson.interval)
                    putExtra(LAST_LESSON_STATUS, true)
                    putExtra(LESSON__CHAT_ID, lesson.chatId)
                    putExtra(CHAT_ROOM_ID, lesson.chatId)

                })
                finish()
            }
        }.launch(
            LessonCompletedActivity.getActivityUnlockNextClassIntent(
                this,
                lesson
            )
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0

            viewModel.getLesson(lessonId)
            viewModel.getQuestions(lessonId)
        }
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        viewModel.lessonLiveData.value?.let {
            resultIntent.putExtra(CHAT_ROOM_ID, it.chatId)
            resultIntent.putExtra(LAST_LESSON_INTERVAL, it.interval)
            resultIntent.putExtra(LAST_LESSON_STATUS, it.status?.name)
            resultIntent.putExtra(LESSON_NUMBER, it.lessonNo)
        }
        setResult(RESULT_OK, resultIntent)
        this@LessonActivity.finish()
    }

    companion object {
        private const val LESSON_ID = "lesson_id"
        const val LAST_LESSON_STATUS = "last_lesson_status"

        fun getActivityIntent(
            context: Context,
            lessonId: Int,
        ) = Intent(context, LessonActivity::class.java).apply {
            putExtra(LESSON_ID, lessonId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }
}
