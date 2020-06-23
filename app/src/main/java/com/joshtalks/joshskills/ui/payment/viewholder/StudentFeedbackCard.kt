package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.server.course_detail.Feedback
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import de.hdodenhof.circleimageview.CircleImageView
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


@Layout(R.layout.layout_listitem_story)
class StudentFeedbackCard(
    private var feedback: Feedback,
    private val context: Context = AppObjectController.joshApplication
) {

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
        setImageView(feedback.thumbnailUrl)
    }

    private fun setImageView(url: String) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(circleImage)
    }

    @Click(R.id.image_circle)
    fun onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            frameLayout.background.setTint(context.resources.getColor(R.color.dark_grey, null))
        }
        VideoPlayerActivity.startVideoActivity(
            context,
            feedback.name,
            "123",
            feedback.videoUrl
        )
        showToast("Clicked Toast")
    }
}