package com.joshtalks.joshskills.ui.view_holders

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DD_MM_YYYY
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.StartCertificationExamEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference
import java.util.Locale


@Layout(R.layout.certification_exam_layout)
class CertificationExamViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.sub_root_view)
    lateinit var subRootView: MaterialCardView

    @View(R.id.message_view)
    lateinit var messageView: ConstraintLayout

    @View(R.id.tv_title)
    lateinit var tvTitle: AppCompatTextView

    @View(R.id.tv_code)
    lateinit var tvCEamCode: AppCompatTextView

    @View(R.id.tv_eligibility_date)
    lateinit var eligibilityDateTV: AppCompatTextView

    @View(R.id.iv_award)
    lateinit var ivAward: AppCompatImageView

    @View(R.id.tv_marks)
    lateinit var tvMarks: AppCompatTextView

    @View(R.id.tv_attempt_left)
    lateinit var tvAttemptLeft: AppCompatTextView

    @View(R.id.tv_attempted_date)
    lateinit var tvAttemptedDate: AppCompatTextView

    @View(R.id.btn_start_exam)
    lateinit var btnStartExam: MaterialButton


    lateinit var viewHolder: CertificationExamViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        ivAward.visibility = android.view.View.INVISIBLE
        tvCEamCode.visibility = android.view.View.GONE
        eligibilityDateTV.visibility = android.view.View.GONE
        tvMarks.visibility = android.view.View.GONE
        tvAttemptLeft.visibility = android.view.View.GONE
        tvAttemptedDate.visibility = android.view.View.GONE

        message.question?.run {
            tvTitle.text = title
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
    }

    private fun updateView(cexamDetail: CertificationExamDetailModel?) {
        cexamDetail?.run {
            tvTitle.text = text

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
                    tvCEamCode.text = getAppContext().getString(R.string.cexam_code, code)
                    tvCEamCode.visibility = android.view.View.VISIBLE
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

    @Click(R.id.message_view)
    fun onClickMessageView() {
        analyzeAction()
    }

    @Click(R.id.btn_start_exam)
    fun btnStartExam() {
        analyzeAction()
    }

    private fun analyzeAction() {
        message.question?.run {
            if (cexamDetail != null) {
                cexamDetail?.examStatus?.let {
                    when {
                        CExamStatus.PASSED == it -> {
                            publishEvent(CExamStatus.CHECK_RESULT)
                        }
                        CExamStatus.ATTEMPTED == it -> {
                            publishEvent(CExamStatus.REATTEMPTED)
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
            RxBus2.publish(
                StartCertificationExamEventBus(
                    message.conversationId,
                    message.chatId,
                    it,
                    examStatus
                )
            )
        }
    }

    private fun getFreshGridentDrawable(): GradientDrawable {
        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#FFE82A"),
            Color.parseColor("#FFE82A"),
            Color.parseColor("#FFE82A")
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
            Color.parseColor("#17C95A"),
            Color.parseColor("#17C95A"),
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