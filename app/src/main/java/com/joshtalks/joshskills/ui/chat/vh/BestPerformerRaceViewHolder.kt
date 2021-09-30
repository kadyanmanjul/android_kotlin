package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModel
import com.joshtalks.joshskills.repository.local.entity.AwardTypes
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.assessment.view.Stub

class BestPerformerRaceViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val subRootView: ConstraintLayout = view.findViewById(R.id.root_view_fl)
    private val tvTitle: JoshTextView = view.findViewById(R.id.tv_title)
    private val thumbnailImage: AppCompatImageView = view.findViewById(R.id.thumbnail_image)
    private val playImage: AppCompatImageView = view.findViewById(R.id.play_icon)
    private var message: ChatModel? = null

    init {
        messageView.also {
            it.setOnClickListener {
                analyzeAction(cardClick = true)
            }
        }
        btnStartExam.also {
            it.setOnClickListener {
                analyzeAction()
            }
        }
    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
        ivAward.visibility = View.INVISIBLE
        eligibilityDateTV.visibility = View.GONE
        tvMarks.visibility = View.GONE
        tvAttemptLeft.visibility = View.GONE
        tvAttemptedDate.visibility = View.GONE

        message.question?.let { question ->
            tvTitle.text =
                HtmlCompat.fromHtml(question.title ?: EMPTY, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (question.cexamDetail == null) {
                setDefaultBg()
                DatabaseUtils.getCExamDetails(
                    conversationId = message.conversationId,
                    question.certificateExamId ?: -1
                ) {
                    question.cexamDetail = it
                    updateView(it)
                }
            } else {
                updateView(question.cexamDetail)
            }
        }

        checkIsExamResume(message)
    }

    override fun unBind() {

    }


    private fun checkIsExamResume(message: ChatModel) {
        val examStatus = message.question?.cexamDetail?.examStatus ?: CExamStatus.FRESH
        val obj =
            CertificationQuestionModel.getResumeExam(message.question?.certificateExamId ?: -1)
        if (CExamStatus.PASSED != examStatus && obj != null) {
            btnStartExam.text = getAppContext().getString(R.string.resume_examination)
        }
    }

    private fun updateView(cexamDetail: CertificationExamDetailModel?) {
        cexamDetail?.run {
            tvTitle.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            when (examStatus) {
                CExamStatus.PASSED -> {
                    tvTitle.setTextColor(ContextCompat.getColor(getAppContext(), R.color.white))

                    tvMarks.text = getAppContext().getString(R.string.cexam_marks, marks.toInt())
                    tvMarks.setTextColor(ContextCompat.getColor(getAppContext(), R.color.white))
                    tvMarks.visibility = android.view.View.VISIBLE

                    tvAttemptedDate.setTextColor(
                        ContextCompat.getColor(
                            getAppContext(),
                            R.color.white
                        )
                    )
                    tvAttemptedDate.visibility = android.view.View.VISIBLE
                    tvAttemptedDate.text =
                        getAppContext().getString(R.string.cexam_passed_on, passedOn)

                    ivAward.visibility = android.view.View.VISIBLE
                    btnStartExam.text = getAppContext().getString(R.string.cexam_check_results)

                    messageView.background = getAttemptedGradientDrawable()
                    subRootView.setCardBackgroundColor(Color.parseColor("#17C95A"))
                }
                CExamStatus.ATTEMPTED -> {
                    tvMarks.text = getAppContext().getString(R.string.cexam_marks, marks.toInt())
                    tvMarks.visibility = android.view.View.VISIBLE

                    tvAttemptedDate.text =
                        getAppContext().getString(R.string.cexam_attempted_on, attemptOn)
                    tvAttemptedDate.visibility = android.view.View.VISIBLE

                    tvAttemptLeft.text =
                        getAppContext().getString(R.string.cexam_attempt_left, attemptLeft)
                    tvAttemptLeft.visibility = android.view.View.VISIBLE
                    btnStartExam.text = getAppContext().getString(R.string.cexam_reattempt)
                    setDefaultBg()
                }
                else -> {
                    btnStartExam.text = getAppContext().getString(R.string.start_examination)
                    //        tvCEamCode.text = getAppContext().getString(R.string.cexam_code, code)
                    //      tvCEamCode.visibility = android.view.View.VISIBLE
                    eligibilityDateTV.visibility = android.view.View.VISIBLE
                    ivAward.visibility = android.view.View.VISIBLE
                    setDefaultBg()
                }
            }
        }
    }

    private fun setDefaultBg() {
        message?.let {
            eligibilityDateTV.text = getAppContext().getString(
                R.string.cexam_date,
                DD_MM_YYYY.format(it.created).toLowerCase(Locale.getDefault())
            )
        }
        messageView.background = getFreshGridentDrawable()
        subRootView.setCardBackgroundColor(Color.parseColor("#FFE82A"))

    }

    private fun analyzeAction(cardClick: Boolean = false) {
        message?.question?.run {
            if (cexamDetail != null) {
                cexamDetail?.examStatus?.let {
                    when {
                        CExamStatus.PASSED == it -> {
                            if (cardClick) {
                                publishEvent(CExamStatus.FRESH)
                            } else {
                                publishEvent(CExamStatus.CHECK_RESULT)
                            }
                        }
                        CExamStatus.ATTEMPTED == it -> {
                            publishEvent(CExamStatus.FRESH)//publishEvent(CExamStatus.REATTEMPTED)
                        }
                        else -> {
                            publishEvent(CExamStatus.FRESH)
                        }
                    }
                }
            } else {
                publishEvent(CExamStatus.FRESH)
            }
        }
    }

    private fun publishEvent(examStatus: CExamStatus) {
        message?.let {
            it.question?.certificateExamId?.let { cexamId ->
                RxBus2.publish(
                    StartCertificationExamEventBus(
                        it.conversationId,
                        it.chatId,
                        cexamId,
                        examStatus,
                        it.question?.interval
                    )
                )
            }
        }
    }

    private fun getFreshGridentDrawable(): GradientDrawable {
        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#AAFFF284"),
            Color.parseColor("#CCFEEC56"),
            Color.parseColor("#FEEC56")
        )
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TR_BL, colors
        )
        gd.cornerRadius = 10f
        return gd
    }

    private fun getAttemptedGradientDrawable(): GradientDrawable {
        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#AA17C95A"),
            Color.parseColor("#CC17C95A"),
            Color.parseColor("#17C95A")
        )
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TR_BL, colors
        )
        gd.cornerRadius = 10f
        return gd
    }
}
