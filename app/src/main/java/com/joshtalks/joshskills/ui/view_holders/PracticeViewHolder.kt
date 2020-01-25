package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
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


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        message.question?.run {
            titleTv.text = this.title
        }
    }

    @Click(R.id.root_view)
    fun onClickRootView() {
        RxBus2.publish(PractiseSubmitEventBus(this, message))
    }


}