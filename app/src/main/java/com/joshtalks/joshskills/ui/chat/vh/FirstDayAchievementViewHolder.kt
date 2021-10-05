package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.airbnb.lottie.LottieAnimationView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.OpenBestPerformerRaceEventBus

class FirstDayAchievementViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val subRootView: ConstraintLayout = view.findViewById(R.id.root_view_fl)
    private val tvTitle: AppCompatTextView = view.findViewById(R.id.date)
    private val thumbnailImage: AppCompatImageView = view.findViewById(R.id.thumbnail_image)
    private val playImage: AppCompatImageView = view.findViewById(R.id.play_icon)
    private val badge: LottieAnimationView = view.findViewById(R.id.badge)
    private var message: ChatModel? = null

    init {
        playImage.also {
            it.setOnClickListener {
                publishEvent()
            }
        }
    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message

        message?.let { message ->
            tvTitle.text =
                HtmlCompat.fromHtml(
                    message.text ?: EMPTY,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            val urlList = message.url?.split('$')

            if (urlList.isNullOrEmpty().not() && urlList?.size!! > 1) {
                badge.playAnimation()
                thumbnailImage.setRoundImage(urlList.get(1))
            }
        }
    }

    override fun unBind() {

    }

    private fun publishEvent() {
        message?.run {
            val urlList = this.url?.split('$')
            if (urlList.isNullOrEmpty().not()) {
                RxBus2.publish(OpenBestPerformerRaceEventBus(urlList?.get(0) ?: EMPTY,true))
            }
        }
    }
}
