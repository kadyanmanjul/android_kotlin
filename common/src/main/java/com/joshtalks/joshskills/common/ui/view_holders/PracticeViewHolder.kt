package com.joshtalks.joshskills.common.ui.view_holders

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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils
import java.lang.ref.WeakReference



class PracticeViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    
    lateinit var rootView: FrameLayout

    
    lateinit var subRootView: FrameLayout

    
    lateinit var titleTv: AppCompatTextView

    
    lateinit var practiceStatusTv: AppCompatTextView

    
    lateinit var subTitleTV: AppCompatTextView

    
    lateinit var receivedMessageTime: AppCompatTextView

    
    lateinit var imageView: AppCompatImageView

    
    lateinit var tvSubmitAnswer: MaterialTextView

    
    lateinit var messageView: ConstraintLayout

    lateinit var viewHolder: PracticeViewHolder

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/JoshOpenSans-Bold.ttf"
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
                R.color.pure_grey
            )
        )
        subTitleTV.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.pure_grey
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
                setResourceInImageView(imageView, R.drawable.ic_wave_special_new)
                rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)
                subRootView.layoutParams = layoutP
                subRootView.setBackgroundResource(R.drawable.incoming_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.pure_white
                    )
                )
            } else {
                sBuilder.append(getAppContext().getString(R.string.submitted))
                tvSubmitAnswer.visibility = android.view.View.GONE
                setResourceInImageView(imageView, R.drawable.ic_wave_special_new)
                rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
                layoutP.gravity = android.view.Gravity.END
                subRootView.layoutParams = layoutP
                subRootView.setBackgroundResource(R.drawable.outgoing_message_same_bg)
                messageView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.surface_success
                    )
                )
                sBuilder.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.success
                        )
                    ),
                    8,
                    sBuilder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                practiceStatusTv.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.surface_success
                    )
                )
                subTitleTV.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.surface_success
                    )
                )

            }
        }
        sBuilder.setSpan(typefaceSpan, 8, sBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        practiceStatusTv.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }


    
    fun onClickRootView() {
        //RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}
