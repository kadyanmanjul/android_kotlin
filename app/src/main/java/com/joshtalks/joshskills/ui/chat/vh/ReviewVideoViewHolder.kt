package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.extension.setImageFromUrl
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.base.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.base.local.entity.ChatModel
import com.joshtalks.joshskills.base.local.eventbus.OpenBestPerformerRaceEventBus

class ReviewVideoViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val subRootView: MaterialCardView = view.findViewById(R.id.root_view_fl)
    private val tvTitle: AppCompatTextView = view.findViewById(R.id.date)
    private val tvDay: AppCompatTextView = view.findViewById(R.id.first_day_text)
    private val thumbnailImage: AppCompatImageView = view.findViewById(R.id.thumbnail_image)
    private val playImage: AppCompatImageView = view.findViewById(R.id.play_icon)
    private val badge: LottieAnimationView = view.findViewById(R.id.badge)
    private var message: ChatModel? = null

    init {
        subRootView.also {
            it.setOnClickListener {
                publishEvent()
            }
        }
    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message

        if (message.type == BASE_MESSAGE_TYPE.FIRST_DAY_ACHIEVEMENT) {
            tvDay.text = "मेरा पहला दिन"
        } else if (message.type == BASE_MESSAGE_TYPE.FIRST_WEEK_ACHIEVEMENT) {
            tvDay.text = "मेरा पहला हफ्ता"
        }

        message?.let { message ->
            tvTitle.text =
                HtmlCompat.fromHtml(
                    message.text ?: EMPTY,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            val urlList = message.url?.split('$')

            if (urlList.isNullOrEmpty().not() && urlList?.size!! > 1) {
                badge.playAnimation()
                thumbnailImage.setImageFromUrl(urlList.get(1))
            }
        }
    }

    override fun unBind() {

    }

    private fun publishEvent() {
        message?.run {
            val urlList = this.url?.split('$')
            if (urlList.isNullOrEmpty().not()) {
                RxBus2.publish(OpenBestPerformerRaceEventBus(
                    chatObj = this,
                    videoUrl = urlList?.get(0) ?: EMPTY,
                    isSharable = true,
                    sharedItem = "DAY_IN_REVIEW"
                ))
            }
        }
    }
}
