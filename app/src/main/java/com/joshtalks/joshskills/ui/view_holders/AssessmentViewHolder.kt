package com.joshtalks.joshskills.ui.view_holders

import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentStartEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference

@Layout(R.layout.assessment_item_layout)
class AssessmentViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.btn_start)
    lateinit var btnStart: MaterialButton

    @View(R.id.tv_title)
    lateinit var title: AppCompatTextView

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: FrameLayout

    lateinit var viewHolder: AssessmentViewHolder


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }
        val layoutP = subRootView.layoutParams as FrameLayout.LayoutParams
        message.question?.let { question ->
            question.title?.run {
                title.text = this
            }

            question.type.let {
                when (it) {
                    BASE_MESSAGE_TYPE.QUIZ -> {
                        btnStart.text = getAppContext().getString(R.string.start_quiz)
                    }
                    BASE_MESSAGE_TYPE.TEST -> {
                        btnStart.text = getAppContext().getString(R.string.conversational_practise)
                    }
                    else -> {
                        btnStart.text = getAppContext().getString(R.string.practice)
                    }
                }
            }
            Log.e("count",""+question.cPractiseCount)
            if (question.cPractiseCount <= 0) {
                layoutP.gravity = android.view.Gravity.START
                rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)
                subRootView.setBackgroundResource(R.drawable.incoming_message_same_bg)
            }else{
                rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
                layoutP.gravity = android.view.Gravity.END
                subRootView.setBackgroundResource(R.drawable.outgoing_message_same_bg)
            }
            subRootView.layoutParams = layoutP

        }

    }


    @Click(R.id.root_sub_view)
    fun onClickRootView() {
        RxBus2.publish(AssessmentStartEventBus(message.question?.assessmentId ?: 0))
    }

    @Click(R.id.btn_start)
    fun onClickStartView() {
        RxBus2.publish(AssessmentStartEventBus(message.question?.assessmentId ?: 0))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}