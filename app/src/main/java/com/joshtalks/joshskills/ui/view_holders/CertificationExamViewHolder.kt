package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DD_MM_YYYY
import com.joshtalks.joshskills.messaging.RxBus2
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

    @View(R.id.message_view)
    lateinit var messageView: ConstraintLayout

    @View(R.id.tv_title)
    lateinit var tvTitle: AppCompatTextView

    @View(R.id.tv_code)
    lateinit var tvCEamCode: AppCompatTextView

    @View(R.id.tv_eligibility_date)
    lateinit var eligibilityDate: AppCompatTextView

    @View(R.id.iv_award)
    lateinit var ivAward: AppCompatImageView

    lateinit var viewHolder: CertificationExamViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        message.question?.run {
            tvTitle.text = title
        }
        eligibilityDate.text = getAppContext().getString(
            R.string.cexam_date,
            DD_MM_YYYY.format(message.created).toLowerCase(Locale.getDefault())
        )
    }

    @Click(R.id.message_view)
    fun onClickMessageView() {
        message.question?.certificateExamId?.let {
            RxBus2.publish(StartCertificationExamEventBus(it))
        }
    }

    @Click(R.id.btn_start_exam)
    fun btnStartExam() {
        message.question?.certificateExamId?.let {
            RxBus2.publish(StartCertificationExamEventBus(it))
        }
    }


    override fun getRoot(): FrameLayout {
        return rootView
    }


}