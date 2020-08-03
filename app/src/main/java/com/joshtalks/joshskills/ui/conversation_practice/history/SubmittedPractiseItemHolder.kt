package com.joshtalks.joshskills.ui.conversation_practice.history


import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.RequestAudioPlayEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.mindorks.placeholderview.annotations.Animate
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import java.text.SimpleDateFormat


private val DD_MM_YYYY = SimpleDateFormat("dd/MM/yyyy")

@Animate(Animate.CARD_BOTTOM_IN_ASC, duration = 1000)
@Layout(R.layout.submitted_practise_item_layout)
class SubmittedPractiseItemHolder(
    private var postion: Int,
    private var data: SubmittedConversationPractiseModel
) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text)
    lateinit var text: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.date)
    lateinit var date: AppCompatTextView

    var context = AppObjectController.joshApplication


    @Resolve
    fun onResolved() {
        title.text = data.title
        text.text = data.text
        date.text = DD_MM_YYYY.format(data.created.time)
        if (data.isPlaying) {
            playing()
        } else {
            notPlaying()
        }

    }

    private fun playing() {
        val greyFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.artboard_color),
            PorterDuff.Mode.MULTIPLY
        )
        /* myLayout.getBackground().setColorFilter(greyFilter)
         myImageView.setColorFilter(greyFilter)
         myTextView.setTextColor(-0x888889)*/
    }

    private fun notPlaying() {

    }


    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(RequestAudioPlayEventBus(postion, data.answerAudioUrl, data.duration))
    }
}