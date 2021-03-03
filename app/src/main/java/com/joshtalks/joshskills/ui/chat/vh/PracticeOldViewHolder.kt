package com.joshtalks.joshskills.ui.chat.vh

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.extension.setResourceImageDefault
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.PractiseSubmitEventBus
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils

class PracticeOldViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val subRootView: FrameLayout = view.findViewById(R.id.root_sub_view)
    private val titleView: AppCompatTextView = view.findViewById(R.id.tv_title)
    private val receivedMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)
    private val practiceStatusTv: AppCompatTextView = view.findViewById(R.id.status_tv)
    private val imageView: AppCompatImageView = view.findViewById(R.id.image_view)
    private val tvSubmitAnswer: MaterialTextView = view.findViewById(R.id.tv_submit_answer)
    private val subTitleTV: AppCompatTextView = view.findViewById(R.id.sub_title_tv)
    private val titleTv: AppCompatTextView = view.findViewById(R.id.tv_title)
    private val messageView: ConstraintLayout = view.findViewById(R.id.message_view)
    private var message: ChatModel? = null

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/OpenSans-Bold.ttf"
            )
        )

    init {
        subRootView.also { it ->
            it.setOnClickListener {
                message?.let {
                    RxBus2.publish(PractiseSubmitEventBus(it))
                }
            }
        }
    }


    override fun bind(message: ChatModel, previousChatModel: ChatModel?) {
        this.message = message
        if (null != message.sender) {
            setViewHolderBG(previousChatModel?.sender, message.sender!!, subRootView)
        }
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
        val sBuilder = SpannableStringBuilder().append("Status: ")
        practiceStatusTv.text = getAppContext().getString(R.string.answer_not_submitted)
        tvSubmitAnswer.visibility = View.VISIBLE
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
        //  layoutP.width  = Utils.dpToPx(getAppContext(), 270f)

        receivedMessageTime.text = Utils.messageTimeConversion(message.created)

        message.question?.run {
            subTitleTV.text = this.title
            this.practiceNo?.let {
                titleTv.text = getAppContext().getString(R.string.practice).plus(" #$it")
            }

            val layoutP = subRootView.layoutParams as FrameLayout.LayoutParams

            if (this.practiceEngagement.isNullOrEmpty()) {
                sBuilder.append(getAppContext().getString(R.string.pending))
                layoutP.gravity = android.view.Gravity.START
                imageView.setResourceImageDefault(R.drawable.ic_pattern)
                //   rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)
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
                tvSubmitAnswer.visibility = View.GONE
                imageView.setResourceImageDefault(R.drawable.ic_practise_submit_bg)
                // rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
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

    override fun unBind() {

    }


}