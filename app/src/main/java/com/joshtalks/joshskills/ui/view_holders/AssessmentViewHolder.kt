package com.joshtalks.joshskills.ui.view_holders

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentStartEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils
import java.lang.ref.WeakReference

@Layout(R.layout.assessment_item_layout)
class AssessmentViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: FrameLayout

    @View(R.id.message_view)
    lateinit var messageView: ConstraintLayout

    @View(R.id.status_tv)
    lateinit var practiceStatusTv: AppCompatTextView

    @View(R.id.btn_start)
    lateinit var btnStart: MaterialTextView

    @View(R.id.tv_title)
    lateinit var title: AppCompatTextView

    @View(R.id.text_message_time)
    lateinit var receivedMessageTime: AppCompatTextView

    lateinit var viewHolder: AssessmentViewHolder

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/OpenSans-Bold.ttf"
            )
        )

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        val sBuilder = SpannableStringBuilder().append("Status: ")
        val layoutP = subRootView.layoutParams as FrameLayout.LayoutParams

        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
        updateTime(receivedMessageTime)

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
            if (question.vAssessmentCount <= 0) {
                sBuilder.append("Pending")
                layoutP.gravity = android.view.Gravity.START
                subRootView.layoutParams = layoutP
                rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)
                subRootView.setBackgroundResource(R.drawable.incoming_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.white
                    )
                )
                practiceStatusTv.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.pdf_bg_color
                    )
                )
            } else {
                sBuilder.append("Submitted")
                layoutP.gravity = android.view.Gravity.END
                subRootView.layoutParams = layoutP
                rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
                subRootView.setBackgroundResource(R.drawable.outgoing_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.sent_msg_background
                    )
                )
                practiceStatusTv.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.bg_green_80
                    )
                )
                sBuilder.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.green
                        )
                    ),
                    8,
                    sBuilder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        sBuilder.setSpan(typefaceSpan, 8, sBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        practiceStatusTv.setText(sBuilder, TextView.BufferType.SPANNABLE)
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