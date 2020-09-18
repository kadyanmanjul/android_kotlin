package com.joshtalks.joshskills.ui.conversation_practice.history


import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.PlayerUtil
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.RequestAudioPlayEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.text.SimpleDateFormat


private val DD_MM_YYYY = SimpleDateFormat("dd/MM/yyyy")

@Layout(R.layout.submitted_practise_item_layout)
class SubmittedPractiseItemHolder(
    private var postion: Int,
    private var data: SubmittedConversationPractiseModel
) {

    @View(R.id.root_view)
    lateinit var cardView: CardView

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.duration)
    lateinit var duration: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.date)
    lateinit var date: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.iv_share)
    lateinit var ivShare: AppCompatImageView

    var context = AppObjectController.joshApplication


    @Resolve
    fun onResolved() {
        title.text = data.title
        duration.text = PlayerUtil.toTimeSongString(data.duration)
        date.text = DD_MM_YYYY.format(data.created.time)
        if (data.isPlaying) {
            playing()
        } else {
            notPlaying()
        }

    }

    private fun playing() {
        val greyFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.light_blue),
            PorterDuff.Mode.MULTIPLY
        )
        cardView.background.colorFilter = greyFilter
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.button_color))
        )

    }

    private fun notPlaying() {
        val greyFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.artboard_color),
            PorterDuff.Mode.MULTIPLY
        )
        cardView.background.colorFilter = greyFilter
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_grey))
        )
    }


    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(RequestAudioPlayEventBus(postion, data.answerAudioUrl, data.duration))
    }
}