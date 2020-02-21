package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.PractiseSubmitEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


@Layout(R.layout.practice_layout)
class PracticeViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.tv_title)
    lateinit var titleTv: AppCompatTextView

    @View(R.id.tv_practice_status)
    lateinit var practiceStatusTv: AppCompatTextView

    @View(R.id.tv_submit_answer)
    lateinit var tvSubmitAnswer: MaterialButton


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        practiceStatusTv.text = activityRef.get()?.getString(R.string.answer_not_submitted)
        practiceStatusTv.setTextColor(ContextCompat.getColor(getAppContext(), R.color.error_color))
        tvSubmitAnswer.visibility = android.view.View.VISIBLE

        message.question?.run {
            titleTv.text = this.title
            if (this.practiceEngagement.isNullOrEmpty().not()) {
                practiceStatusTv.text = activityRef.get()?.getString(R.string.answer_submitted)
                practiceStatusTv.setTextColor(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.color_success
                    )
                )
                tvSubmitAnswer.visibility = android.view.View.GONE
            }

        }
    }

    @Click(R.id.root_view)
    fun onClickRootView() {
        RxBus2.publish(PractiseSubmitEventBus(this, message))

    }

    @Click(R.id.tv_submit_answer)
    fun onClickButton() {
        RxBus2.publish(PractiseSubmitEventBus(this, message))

    }


}