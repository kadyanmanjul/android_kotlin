package com.joshtalks.joshskills.common.ui.view_holders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.DD_MM_YYYY
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.DatabaseUtils
import com.joshtalks.joshskills.common.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.common.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.eventbus.StartCertificationExamEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestionModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference
import java.util.*



class CertificationExamViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {


    lateinit var rootView: FrameLayout


    lateinit var subRootView: MaterialCardView


    lateinit var messageView: ConstraintLayout


    lateinit var tvTitle: JoshTextView

    /*
    lateinit var tvCEamCode: AppCompatTextView*/


    lateinit var eligibilityDateTV: AppCompatTextView


    lateinit var ivAward: AppCompatImageView


    lateinit var tvMarks: AppCompatTextView


    lateinit var tvAttemptLeft: AppCompatTextView


    lateinit var tvAttemptedDate: AppCompatTextView


    lateinit var btnStartExam: MaterialTextView


    lateinit var viewHolder: CertificationExamViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        ivAward.visibility = android.view.View.INVISIBLE
        //   tvCEamCode.visibility = android.view.View.GONE
        eligibilityDateTV.visibility = android.view.View.GONE
        tvMarks.visibility = android.view.View.GONE
        tvAttemptLeft.visibility = android.view.View.GONE
        tvAttemptedDate.visibility = android.view.View.GONE

        message.question?.run {
            tvTitle.text = HtmlCompat.fromHtml(title ?: EMPTY, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        message.question?.let { question ->
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
        checkIsExamResume()
    }

    private fun checkIsExamResume() {
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
                    tvTitle.setTextColor(ContextCompat.getColor(getAppContext(), R.color.pure_white))

                    tvMarks.text = getAppContext().getString(R.string.cexam_marks, marks.toInt())
                    tvMarks.setTextColor(ContextCompat.getColor(getAppContext(), R.color.pure_white))
                    tvMarks.visibility = android.view.View.VISIBLE

                    tvAttemptedDate.setTextColor(
                        ContextCompat.getColor(
                            getAppContext(),
                            R.color.pure_white
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
        eligibilityDateTV.text = getAppContext().getString(
            R.string.cexam_date,
            DD_MM_YYYY.format(message.created).toLowerCase(Locale.getDefault())
        )
        messageView.background = getFreshGridentDrawable()
        subRootView.setCardBackgroundColor(Color.parseColor("#FFE82A"))
    }

    
    fun onClickMessageView() {
        analyzeAction(cardClick = true)
    }

    
    fun btnStartExam() {
        analyzeAction()
    }

    private fun analyzeAction(cardClick: Boolean = false) {
        message.question?.run {
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
        message.question?.certificateExamId?.let {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                StartCertificationExamEventBus(
                    message.conversationId,
                    message.chatId,
                    it,
                    examStatus,
                    message.question?.interval
                )
            )
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

    override fun getRoot(): FrameLayout {
        return rootView
    }

}