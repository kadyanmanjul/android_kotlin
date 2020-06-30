package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.Feedback
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import de.hdodenhof.circleimageview.CircleImageView


@Layout(R.layout.layout_listitem_story)
class StudentFeedbackCard(
    private var feedback: Feedback,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(CardType.OTHER_INFO, 0) {

    @com.mindorks.placeholderview.annotations.View(R.id.name)
    lateinit var name: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.frameLayout)
    lateinit var frameLayout: FrameLayout

    @com.mindorks.placeholderview.annotations.View(R.id.profession)
    lateinit var profession: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.image_circle)
    lateinit var circleImage: CircleImageView

    @Resolve
    fun onResolved() {
        name.text = feedback.name
        profession.text = feedback.shortDescription
        feedback.thumbnailUrl?.let {
            setDefaultImageView(circleImage,feedback.thumbnailUrl!!)
        }
    }

    @Click(R.id.image_circle)
    fun onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            frameLayout.background.setTint(context.resources.getColor(R.color.dark_grey, null))
        }
        feedback.videoUrl?.let {
            VideoPlayerActivity.startVideoActivity(
                context,
                feedback.name,
                "123",
                feedback.videoUrl
            )
        }

    }
}