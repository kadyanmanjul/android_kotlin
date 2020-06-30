package com.joshtalks.joshskills.ui.view_holders

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.DemoLesson
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.demo_lesson_view_holder)
class DemoLessonViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    val data: DemoLesson,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.txt_title)
    lateinit var txtTitle: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.imageView)
    lateinit var imgView: AppCompatImageView

    @Resolve
    fun onResolved() {
        txtTitle.text = data.title
        data.thumbnailUrl?.let {
            setDefaultImageView(imgView, it)
        }
    }

    @Click(R.id.cardView)
    fun onClick() {
        if (data.videoUrl.isNullOrEmpty()) {
            showToast(getAppContext().getString(R.string.video_url_not_exist))
            return
        }
        VideoPlayerActivity.startVideoActivity(
            context,
            data.title,
            data.videoId,
            data.videoUrl
        )
    }
}
