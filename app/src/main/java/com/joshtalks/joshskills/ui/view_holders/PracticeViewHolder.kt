package com.joshtalks.joshskills.ui.view_holders

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.PractiseSubmitEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils
import java.lang.ref.WeakReference


@Layout(R.layout.practice_layout)
class PracticeViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: FrameLayout

    @View(R.id.tv_title)
    lateinit var titleTv: AppCompatTextView

    @View(R.id.status_tv)
    lateinit var practiceStatusTv: AppCompatTextView

    @View(R.id.sub_title_tv)
    lateinit var subTitleTV: AppCompatTextView

    @View(R.id.text_message_time)
    lateinit var receivedMessageTime: AppCompatTextView

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.tv_submit_answer)
    lateinit var tvSubmitAnswer: MaterialTextView

    @View(R.id.message_view)
    lateinit var messageView: ConstraintLayout

    lateinit var viewHolder: PracticeViewHolder

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/OpenSans-Bold.ttf"
            )
        )

    @SuppressLint("ClickableViewAccessibility")
    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }
        val sBuilder = SpannableStringBuilder().append("Status: ")
        practiceStatusTv.text = activityRef.get()?.getString(R.string.answer_not_submitted)
        tvSubmitAnswer.visibility = android.view.View.VISIBLE
        imageView.backgroundTintList = null
        practiceStatusTv.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.pdf_bg_color
            )
        )
        subTitleTV.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.pdf_bg_color
            )
        )
        val layoutP = subRootView.layoutParams as FrameLayout.LayoutParams
        //  layoutP.width  = Utils.dpToPx(getAppContext(), 270f)

        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
        updateTime(receivedMessageTime)

        message.question?.run {
            subTitleTV.text = this.title
            this.practiceNo?.let {
                titleTv.text = getAppContext().getString(R.string.practice).plus(" #$it")
            }


            if (this.practiceEngagement.isNullOrEmpty()) {
                sBuilder.append(getAppContext().getString(R.string.pending))
                layoutP.gravity = android.view.Gravity.START
                setResourceInImageView(imageView, R.drawable.ic_pattern)
                rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)
                subRootView.layoutParams = layoutP
                subRootView.setBackgroundResource(R.drawable.incoming_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.white
                    )
                )
            } else {
                sBuilder.append(getAppContext().getString(R.string.submitted))
                tvSubmitAnswer.visibility = android.view.View.GONE
                setResourceInImageView(imageView, R.drawable.ic_practise_submit_bg)
                rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
                layoutP.gravity = android.view.Gravity.END
                subRootView.layoutParams = layoutP
                subRootView.setBackgroundResource(R.drawable.outgoing_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.sent_msg_background
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

                practiceStatusTv.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.bg_green_80
                    )
                )
                subTitleTV.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.bg_green_80
                    )
                )

            }
        }
        sBuilder.setSpan(typefaceSpan, 8, sBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        practiceStatusTv.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }


    @Click(R.id.root_sub_view)
    fun onClickRootView() {
        RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}
