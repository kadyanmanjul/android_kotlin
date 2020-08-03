package com.joshtalks.joshskills.ui.view_holders

import android.view.View.GONE
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.UnlockNextClassEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


@Layout(R.layout.unlock_class_item_layout)
class UnlockNextClassViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.btn_start)
    lateinit var btnStart: MaterialButton

    @View(R.id.tv_title)
    lateinit var title: AppCompatTextView


    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: ConstraintLayout

    lateinit var viewHolder: UnlockNextClassViewHolder


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

        message.question?.let { question ->

            title.text = "#Unlock Next Class"
            btnStart.text = "Unlock Next class"
        }
    }


    @Click(R.id.root_sub_view)
    fun onClickRootView() {
        RxBus2.publish(UnlockNextClassEventBus(message.question?.assessmentId ?: 0, viewHolder))
        subRootView.visibility = GONE
    }

    @Click(R.id.btn_start)
    fun onClickStartView() {
        RxBus2.publish(UnlockNextClassEventBus(message.question?.assessmentId ?: 0, viewHolder))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}