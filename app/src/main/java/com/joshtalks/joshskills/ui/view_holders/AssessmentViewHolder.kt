package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.PractiseSubmitEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


@Layout(R.layout.assessment_item_layout)
class AssessmentViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.btn_start)
    lateinit var btnStart: MaterialButton

    @View(R.id.tv_title)
    lateinit var title: AppCompatTextView


    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: ConstraintLayout

    lateinit var viewHolder: AssessmentViewHolder


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

        message.question?.let { question ->
            question.title?.run {
                title.text = this
            }

            question.material_type.let {
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
        }
    }


    @Click(R.id.root_sub_view)
    fun onClickRootView() {
        RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

    @Click(R.id.btn_start)
    fun onClickStartView() {
        RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}