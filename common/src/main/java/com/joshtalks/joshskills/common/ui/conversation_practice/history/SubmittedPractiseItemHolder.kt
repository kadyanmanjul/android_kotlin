package com.joshtalks.joshskills.common.ui.conversation_practice.history


import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.custom_ui.PlayerUtil
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.RequestAudioPlayEventBus
import com.joshtalks.joshskills.common.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.text.SimpleDateFormat


private val DD_MM_YYYY = SimpleDateFormat("dd/MM/yyyy")

class SubmittedPractiseItemHolder(
    private var postion: Int,
    private var data: SubmittedConversationPractiseModel
) {

    
    lateinit var cardView: CardView

    
    lateinit var title: AppCompatTextView

    
    lateinit var duration: AppCompatTextView

    
    lateinit var date: AppCompatTextView

    
    lateinit var imageView: AppCompatImageView

    
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
            ContextCompat.getColor(context, R.color.primary_400),
            PorterDuff.Mode.MULTIPLY
        )
        cardView.background.colorFilter = greyFilter
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_500))
        )

    }

    private fun notPlaying() {
        val greyFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.pure_grey),
            PorterDuff.Mode.MULTIPLY
        )
        cardView.background.colorFilter = greyFilter
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dark_grey))
        )
    }


    
    fun onClick() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(RequestAudioPlayEventBus(postion, data.answerAudioUrl, data.duration))
    }
}