package com.joshtalks.joshskills.ui.view_holders

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
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
class PracticeViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {


    @View(R.id.tv_title)
    lateinit var titleTv: AppCompatTextView

    @View(R.id.status_tv)
    lateinit var practiceStatusTv: AppCompatTextView


    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.root_view)
    lateinit var rootView: CardView

    @View(R.id.tv_submit_answer)
    lateinit var tvSubmitAnswer: MaterialTextView

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
        val layoutP = rootView.layoutParams as FrameLayout.LayoutParams
        rootView.setCardBackgroundColor(ContextCompat.getColor(getAppContext(), R.color.white))

        val sBuilder = SpannableStringBuilder().append("Status: ")
        practiceStatusTv.text = activityRef.get()?.getString(R.string.answer_not_submitted)
        tvSubmitAnswer.visibility = android.view.View.VISIBLE
        message.question?.run {
            titleTv.text = this.title
            if (this.practiceEngagement.isNullOrEmpty()) {
                sBuilder.append("Pending")
                layoutP.height = Utils.dpToPx(getAppContext(), 190f)
                layoutP.width = Utils.dpToPx(getAppContext(), 260f)
                layoutP.gravity = android.view.Gravity.START
                setResourceInImageView(imageView, R.drawable.ic_pattern)
                rootView.layoutParams = layoutP

            } else {
                sBuilder.append("Submitted")
                tvSubmitAnswer.visibility = android.view.View.GONE
                rootView.setContentPadding(
                    Utils.dpToPx(getAppContext(), 5f),
                    Utils.dpToPx(getAppContext(), 5f),
                    Utils.dpToPx(getAppContext(), 5f),
                    Utils.dpToPx(getAppContext(), 5f)
                )
                setResourceInImageView(imageView, R.drawable.ic_practise_submit_bg)
                layoutP.gravity = android.view.Gravity.END
                layoutP.height = Utils.dpToPx(getAppContext(), 160f)
                layoutP.width = Utils.dpToPx(getAppContext(), 260f)
                rootView.layoutParams = layoutP
                rootView.setCardBackgroundColor(Color.parseColor("#EFFAFF"))
                sBuilder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#25d366")),
                    8,
                    sBuilder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        sBuilder.setSpan(typefaceSpan, 8, sBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        practiceStatusTv.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }



    @Click(R.id.root_view)
    fun onClickRootView() {
        RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

    @Click(R.id.status_tv)
    fun onClickStatusView() {
        RxBus2.publish(PractiseSubmitEventBus(viewHolder, message))
    }

}