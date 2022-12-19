package com.joshtalks.joshskills.certificate.examview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.common.core.interfaces.CertificationExamListener
import com.joshtalks.joshskills.common.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationExamView
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.certificate.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.certificate.CertificationExamViewModel
import com.joshtalks.joshskills.certificate.R
import com.joshtalks.joshskills.certificate.questionlistbottom.Callback
import com.joshtalks.joshskills.certificate.questionlistbottom.QuestionListBottomSheet
import kotlinx.android.synthetic.main.activity_cexam_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

const val ARG_EXAM_VIEW = "exam_view"
const val ARG_OPEN_QUESTION_ID = "open_question_id"
const val ARG_ATTEMPT_SEQUENCE = "attempt_sequence"
const val EXAM_TYPE = "exam_type"

class CExamMainActivity : BaseActivity(), CertificationExamListener {

    companion object {
        fun startExamActivity(
            context: Context,
            certificationQuestionModel: CertificationQuestionModel,
            examView: CertificationExamView = CertificationExamView.EXAM_VIEW,
            conversationId: String? = null,
            openQuestionId: Int = -1,
            attemptSequence: Int = -1,
            examType: String? = EMPTY
        ): Intent {
            return Intent(context, CExamMainActivity::class.java).apply {
                putExtra(CERTIFICATION_EXAM_QUESTION, certificationQuestionModel)
                putExtra(ARG_EXAM_VIEW, examView)
                putExtra(ARG_OPEN_QUESTION_ID, openQuestionId)
                putExtra(ARG_ATTEMPT_SEQUENCE, attemptSequence)
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(EXAM_TYPE, examType)
            }
        }
    }

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this)[CertificationExamViewModel::class.java]
    }
    private var certificationQuestionModel: CertificationQuestionModel? = null
    private var countdownTimerBack: CountdownTimerBack? = null
    private var examView: CertificationExamView = CertificationExamView.EXAM_VIEW
    private var openQuestionId: Int = 0
    private var attemptSequence: Int = -1
    private val questionsList: ArrayList<CertificationQuestion> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cexam_main)
        setupUI()
        certificationQuestionModel =
            intent.getParcelableExtra(CERTIFICATION_EXAM_QUESTION) as CertificationQuestionModel?
        examView =
            (intent.getSerializableExtra(ARG_EXAM_VIEW) as CertificationExamView?)
                ?: CertificationExamView.EXAM_VIEW
        openQuestionId = intent.getIntExtra(ARG_OPEN_QUESTION_ID, 0)
        attemptSequence = intent.getIntExtra(ARG_ATTEMPT_SEQUENCE, -1)
        viewModel.examType.value = intent.getStringExtra(EXAM_TYPE)
        certificationQuestionModel?.run {
            if (CertificationExamView.EXAM_VIEW == examView) {
                if (lastQuestionOfExit < 0) {
                    questionsList.addAll(questions.shuffled())
                } else {
                    questionsList.addAll(questions)
                }
            } else {
                questionsList.addAll(questions.sortedBy { it.sortOrder })
            }
        }
        addObserver()
        setupViewPager(questionsList)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun addObserver() {
        viewModel.apiStatus.observe(
            this
        ) {
            hideProgressBar()
            if (ApiCallStatus.SUCCESS == it) {
                certificationQuestionModel?.certificateExamId?.let {
                    CertificationQuestionModel.removeResumeExam(it)
                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                }
                this.finish()
            }
        }
    }

    private fun setupUI() {
        iv_bookmark.setOnClickListener {
            questionsList.let { questions ->
                if (questions.size > question_view_pager.currentItem) {
                    questions[question_view_pager.currentItem].let {
                        it.isBookmarked = it.isBookmarked.not()
                        updateBookmarkIV(questions, question_view_pager.currentItem)
                    }
                }
            }
        }
        iv_all_question.setOnClickListener {
            openQuestionListBottomSheet()
        }
        iv_back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun startExamTimer() {
        val lastTime = certificationQuestionModel?.timerTime ?: 0L
        val examTime = certificationQuestionModel?.totalMinutes?.toLong() ?: 0L
        if (lastTime > 0) {
            startTimer(lastTime)
        } else {
            startTimer(TimeUnit.MINUTES.toMillis(examTime) - 100)
        }
    }

    private fun setupViewPager(questions: List<CertificationQuestion>) {
        // This logic for  reduce complexity result screen
        if (CertificationExamView.RESULT_VIEW == examView) {
            questions.forEach { cq ->
                cq.correctOptionId = cq.answers.find { it.isCorrect }?.id ?: -1
                cq.userSelectedOption =
                    cq.userSubmittedAnswer?.find { it.attemptSeq == attemptSequence }?.answerId
                        ?: -1
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)

            val adapter = CExamQuestionAdapter(
                questions, examView,
                object : Callback {
                    override fun onGoToQuestion(position: Int) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(350)
                            if (position == question_view_pager.adapter?.itemCount) {
                                if (CertificationExamView.EXAM_VIEW == examView) {
                                    openQuestionListBottomSheet()
                                }
                            } else {
                                question_view_pager.currentItem = position
                            }
                        }
                    }
                }
            )
            question_view_pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            question_view_pager.offscreenPageLimit =
                if (questions.isNotEmpty()) questions.size else OFFSCREEN_PAGE_LIMIT_DEFAULT
            question_view_pager.setPageTransformer(MarginPageTransformer(Utils.dpToPx(40)))
            question_view_pager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateBookmarkIV(questions, position)
                    val cPage = position + 1
                    tv_question.text = "$cPage/${questions.size}"

                    if (CertificationExamView.EXAM_VIEW == examView) {
                        val obj = questions.findLast { it.isAttempted.not() }
                        if (obj == null && cPage == question_view_pager.adapter?.itemCount) {
                            openQuestionListBottomSheet()
                        }
                    }
                }
            })
            question_view_pager.adapter = adapter
            setupUIAsExamView()
        }
    }

    private fun setupUIAsExamView() {
        if (CertificationExamView.EXAM_VIEW == examView) {
            bottom_bar.visibility = View.VISIBLE
            startExamTimer()
            question_view_pager.currentItem =
                certificationQuestionModel?.lastQuestionOfExit ?: 0
        } else {
            questionsList.indexOfLast { it.questionId == openQuestionId }.let {
                question_view_pager.currentItem = it
            }
            tv_timer.visibility = View.GONE
        }
    }

    private fun updateBookmarkIV(questions: List<CertificationQuestion>, position: Int) {
        if (questions[position].isBookmarked) {
            iv_bookmark.setColorFilter(ContextCompat.getColor(applicationContext, R.color.warning))
        } else {
            iv_bookmark.setColorFilter(ContextCompat.getColor(applicationContext, R.color.pure_white))
        }
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    tv_timer.text = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millis),
                        TimeUnit.MILLISECONDS.toSeconds(millis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                    )
                }
            }

            override fun onTimerFinish() {
                onFinishExam()
            }
        }
        countdownTimerBack?.startTimer()
    }

    override fun onPauseExit() {
        certificationQuestionModel?.timerTime = countdownTimerBack?.remainTime() ?: 0
        certificationQuestionModel?.lastQuestionOfExit = question_view_pager.currentItem
        certificationQuestionModel?.saveForLaterUse()
        val intent = Intent()
        setResult(Activity.RESULT_CANCELED, intent)
        this.finish()
    }

    override fun onFinishExam() {
        certificationQuestionModel?.let {
            showProgressBar()
            viewModel.submitExam(it.certificateExamId, it)
        }
    }

    override fun onClose() {
    }

    override fun onGoToQuestion(position: Int) {
        question_view_pager.currentItem = position
    }

    private fun openQuestionListBottomSheet() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(150)
            val prev =
                supportFragmentManager.findFragmentByTag(QuestionListBottomSheet::class.java.name)
            if (prev != null) {
                return@launch
            }
            questionsList.run {
                val bottomSheetFragment =
                    QuestionListBottomSheet.newInstance(this, question_view_pager.currentItem)
                bottomSheetFragment.show(
                    supportFragmentManager,
                    QuestionListBottomSheet::class.java.name
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (CertificationExamView.EXAM_VIEW == examView) {
            openQuestionListBottomSheet()
            return
        }
        this.finish()
    }
}
