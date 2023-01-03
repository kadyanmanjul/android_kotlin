package com.joshtalks.joshskills.explore.course_details.viewholder

import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.course_details.models.DemoLesson
import com.joshtalks.joshskills.explore.databinding.DemoLessonViewHolderBinding

class DemoLessonViewHolder(
    val item: DemoLessonViewHolderBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            DemoLesson::class.java
        )

        item.txtTitle.text = data.title
        data.video?.video_image_url?.run {
            setDefaultImageView(item.imageView, this)
        }

        item.cardView.setOnClickListener {
            if (data.video == null) {
                showToast(getAppContext().getString(R.string.video_url_not_exist))
                return@setOnClickListener
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
}
