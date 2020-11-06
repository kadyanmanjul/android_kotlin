package com.joshtalks.joshskills.ui.certification_exam.examview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.interfaces.CertificationExamListener
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationExamView
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.ui.certification_exam.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.questionlistbottom.Callback
import com.joshtalks.joshskills.ui.certification_exam.questionlistbottom.QuestionListBottomSheet
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_cexam_main.bottom_bar
import kotlinx.android.synthetic.main.activity_cexam_main.iv_all_question
import kotlinx.android.synthetic.main.activity_cexam_main.iv_back
import kotlinx.android.synthetic.main.activity_cexam_main.iv_bookmark
import kotlinx.android.synthetic.main.activity_cexam_main.question_view_pager
import kotlinx.android.synthetic.main.activity_cexam_main.tv_question
import kotlinx.android.synthetic.main.activity_cexam_main.tv_timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val ARG_EXAM_VIEW = "exam_view"
const val ARG_OPEN_QUESTION_ID = "open_question_id"
const val ARG_ATTEMPT_SEQUENCE = "attempt_sequence"

class CExamMainActivity : BaseActivity(), CertificationExamListener {

    companion object {
        fun startExamActivity(
            context: Context,
            certificationQuestionModel: CertificationQuestionModel,
            examView: CertificationExamView = CertificationExamView.EXAM_VIEW,
            openQuestionId: Int = -1,
            attemptSequence: Int = -1
        ): Intent {
            return Intent(context, CExamMainActivity::class.java).apply {
                putExtra(CERTIFICATION_EXAM_QUESTION, certificationQuestionModel)
                putExtra(ARG_EXAM_VIEW, examView)
                putExtra(ARG_OPEN_QUESTION_ID, openQuestionId)
                putExtra(ARG_ATTEMPT_SEQUENCE, attemptSequence)
            }
        }
    }

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var certificationQuestionModel: CertificationQuestionModel? = null
    private var countdownTimerBack: CountdownTimerBack? = null
    private var examView: CertificationExamView = CertificationExamView.EXAM_VIEW
    private var openQuestionId: Int = 0
    private var attemptSequence: Int = -1


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

        certificationQuestionModel?.run {
            questions.sortedBy { it.sortOrder }
            setupViewPager(questions)
        }
        addObserver()
    }

    private fun addObserver() {
        viewModel.apiStatus.observe(this, {
            hideProgressBar()
            if (ApiCallStatus.SUCCESS == it) {
                certificationQuestionModel?.certificateExamId?.let {
                    CertificationQuestionModel.removeResumeExam(it)
                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                }
                this.finish()
            }
        })

    }

    private fun showProgressBar() {
        FullScreenProgressDialog.showProgressBar(this)
    }

    private fun hideProgressBar() {
        FullScreenProgressDialog.hideProgressBar(this)
    }

    private fun setupUI() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            iv_bookmark.setOnClickListener {
                certificationQuestionModel?.questions?.let { questions ->
                    questions[question_view_pager.currentItem].let {
                        it.isBookmarked = it.isBookmarked.not()
                        updateBookmarkIV(questions, question_view_pager.currentItem)
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
        CoroutineScope(Dispatchers.Main).launch {
            delay(150)

            //This logic for  reduce complexity result screen
            if (CertificationExamView.RESULT_VIEW == examView) {
                questions.forEach { cq ->
                    cq.correctOptionId = cq.answers.find { it.isCorrect }?.id ?: -1
                    cq.userSelectedOption =
                        cq.userSubmittedAnswer?.find { it.attemptSeq == attemptSequence }?.answerId
                            ?: -1
                }
            }
            val adapter = CExamQuestionAdapter(questions, examView, object : Callback {
                override fun onGoToQuestion(position: Int) {
                    question_view_pager.currentItem = position
                }
            })
            question_view_pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            question_view_pager.offscreenPageLimit = questions.size
            question_view_pager.setPageTransformer(MarginPageTransformer(Utils.dpToPx(40)))
            question_view_pager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateBookmarkIV(questions, position)
                    val cPage = position + 1
                    tv_question.text = "$cPage/${questions.size}"
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
            certificationQuestionModel?.questions
                ?.indexOfLast { it.questionId == openQuestionId }
                ?.let {
                    question_view_pager.currentItem = it

                }
            tv_timer.visibility = View.GONE
        }
    }

    private fun updateBookmarkIV(questions: List<CertificationQuestion>, position: Int) {
        if (questions[position].isBookmarked) {
            iv_bookmark.setColorFilter(ContextCompat.getColor(applicationContext, R.color.yellow))
        } else {
            iv_bookmark.setColorFilter(ContextCompat.getColor(applicationContext, R.color.white))
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
        val prev =
            supportFragmentManager.findFragmentByTag(QuestionListBottomSheet::class.java.name)
        if (prev != null) {
            return
        }
        certificationQuestionModel?.questions?.run {
            val bottomSheetFragment =
                QuestionListBottomSheet.newInstance(this, question_view_pager.currentItem)
            bottomSheetFragment.show(
                supportFragmentManager,
                QuestionListBottomSheet::class.java.name
            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
    }

    override fun onBackPressed() {
        if (CertificationExamView.EXAM_VIEW == examView) {
            openQuestionListBottomSheet()
            return
        }
        this.finish()
    }


}