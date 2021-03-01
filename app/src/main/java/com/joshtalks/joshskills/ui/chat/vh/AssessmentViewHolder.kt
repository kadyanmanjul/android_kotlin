package com.joshtalks.joshskills.ui.chat.vh

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
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentStartEventBus
import com.mindorks.placeholderview.annotations.Layout
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils

@Layout(R.layout.assessment_item_layout)
class AssessmentViewHolder(view: android.view.View, userId: String) : BaseViewHolder(view, userId) {

    private val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    private var message: ChatModel? = null
    private val subRootView: FrameLayout = view.findViewById(R.id.root_sub_view)
    private val messageView: ConstraintLayout = view.findViewById(R.id.message_view)
    private val practiceStatusTv: AppCompatTextView = view.findViewById(R.id.status_tv)
    private val btnStart: MaterialTextView = view.findViewById(R.id.btn_start)
    private val title: AppCompatTextView = view.findViewById(R.id.tv_title)
    private val receivedMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)
    private var layoutP: FrameLayout.LayoutParams =
        subRootView.layoutParams as FrameLayout.LayoutParams

    private fun getLeftPaddingForReceiver() = Utils.dpToPx(getAppContext(), 7f)
    private fun getRightPaddingForReceiver() =
        Utils.dpToPx(getAppContext(), 80f)

    private fun getLeftPaddingForSender() = Utils.dpToPx(getAppContext(), 80f)
    private fun getRightPaddingForSender() = Utils.dpToPx(getAppContext(), 7f)
    private val sBuilder = SpannableStringBuilder().append("Status: ")

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/OpenSans-Bold.ttf"
            )
        )


    init {
        subRootView.also {
            it.setOnClickListener {
                fireInTheHole()
            }
        }
        btnStart.also {
            it.setOnClickListener {
                fireInTheHole()
            }
        }
    }

    private fun fireInTheHole() {
        message?.let {
            RxBus2.publish(AssessmentStartEventBus(it.chatId, it.question?.assessmentId ?: 0))
        }
    }


    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
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
                sBuilder.append(getAppContext().getString(R.string.pending))
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
                sBuilder.append(getAppContext().getString(R.string.submitted))
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

    override fun unBind() {

    }


}