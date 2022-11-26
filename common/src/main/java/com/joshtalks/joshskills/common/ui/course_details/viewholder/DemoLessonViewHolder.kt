package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.DemoLesson
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


class DemoLessonViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    val data: DemoLesson,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {


    
    lateinit var txtTitle: JoshTextView

    
    lateinit var imgView: AppCompatImageView

    @Resolve
    fun onResolved() {
        txtTitle.text = data.title
        data.video?.video_image_url?.run {
            setDefaultImageView(imgView, this)
        }
    }

    
    fun onClick() {
        if (data.video == null) {
            showToast(getAppContext().getString(R.string.video_url_not_exist))
            return
        }

        RxBus2.publish(
            VideoShowEvent(
                videoTitle = data.title,
                videoId = data.video.id,
                videoUrl = data.video.video_url,
                videoWidth = data.video.video_width,
                videoHeight = data.video.video_height,
            )
        )
    }
}
