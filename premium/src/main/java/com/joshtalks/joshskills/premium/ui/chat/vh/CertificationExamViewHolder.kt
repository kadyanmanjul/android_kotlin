package com.joshtalks.joshskills.premium.ui.chat.vh

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.DD_MM_YYYY
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.DatabaseUtils
import com.joshtalks.joshskills.premium.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.premium.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.repository.local.eventbus.StartCertificationExamEventBus
import com.joshtalks.joshskills.premium.repository.server.certification_exam.CertificationQuestionModel
import java.util.*

class CertificationExamViewHolder(view: View, userId: String) :
    BaseViewHolder(view, userId) {

    private val subRootView: MaterialCardView = view.findViewById(R.id.sub_root_view)
    private val messageView: ConstraintLayout = view.findViewById(R.id.message_view)
    private val tvTitle: AppCompatTextView = view.findViewById(R.id.tv_title)
    private val tvMarks: AppCompatTextView = view.findViewById(R.id.tv_marks)
    private val tvAttemptedDate: AppCompatTextView = view.findViewById(R.id.tv_attempted_date)
    private val btnStartExam: MaterialTextView = view.findViewById(R.id.btn_start_exam)
    private var message: ChatModel? = null
    private lateinit var attemptsLeft: String
    private lateinit var attemptNo: String
    private lateinit var attemptedOn: String
    private lateinit var getmarks: String

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
        tvMarks.visibility = View.GONE
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
            attemptsLeft = attemptLeft.toString()
            attemptNo = attempted.toString()
            attemptedOn = attemptOn
            getmarks = marks.toString()
            tvTitle.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            when (examStatus) {
                CExamStatus.PASSED -> {
                    subRootView.setCardBackgroundColor(ContextCompat.getColor(getAppContext(),R.color.decorative_two))
                    tvTitle.setTextColor(ContextCompat.getColor(getAppContext(), R.color.pure_white))

                    tvMarks.text = getAppContext().getString(R.string.cexam_marks, marks.toInt())
                    tvMarks.setTextColor(ContextCompat.getColor(getAppContext(), R.color.pure_white))
                    tvMarks.visibility = View.VISIBLE

                    tvAttemptedDate.setTextColor(
                        ContextCompat.getColor(
                            getAppContext(),
                            R.color.pure_white
                        )
                    )
                    tvAttemptedDate.visibility = View.VISIBLE
                    tvAttemptedDate.text =
                        getAppContext().getString(R.string.cexam_passed_on, passedOn)

                    btnStartExam.text = getAppContext().getString(R.string.cexam_check_results)
                    btnStartExam.setTextColor(ContextCompat.getColor(getAppContext(), R.color.pure_white))

                }
                CExamStatus.ATTEMPTED -> {
                    tvMarks.text = "${getAppContext().getString(R.string.cexam_marks, marks.toInt())} (${getAppContext().getString(R.string.cexam_attempt_left, attemptLeft)})"
                    tvMarks.visibility = View.VISIBLE

                    tvAttemptedDate.text =
                        getAppContext().getString(R.string.cexam_attempted_on, attemptOn)
                    tvAttemptedDate.visibility = View.VISIBLE

                    btnStartExam.text = getAppContext().getString(R.string.cexam_reattempt)
                    if (attemptLeft <= 0)
                        btnStartExam.visibility = View.INVISIBLE
                    setDefaultBg()
                }
                else -> {
                    btnStartExam.text = getAppContext().getString(R.string.start_exam)
                    //tvMarks.visibility = View.VISIBLE
                    tvAttemptedDate.visibility = View.VISIBLE
                    //tvMarks.text = code
                    tvAttemptedDate.text = getAppContext().getString(R.string.cexam_date,eligibilityDate)
                    //        tvCEamCode.text = getAppContext().getString(R.string.cexam_code, code)
                    //      tvCEamCode.visibility = android.view.View.VISIBLE
                    setDefaultBg()
                }
            }
        }
    }

    private fun setDefaultBg() {
        subRootView.setCardBackgroundColor(ContextCompat.getColor(subRootView.context,R.color.warning))

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