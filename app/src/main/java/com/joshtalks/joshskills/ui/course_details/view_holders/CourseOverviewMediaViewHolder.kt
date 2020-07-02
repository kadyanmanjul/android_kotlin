package com.joshtalks.joshskills.ui.course_details.view_holders

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
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
import com.joshtalks.joshskills.repository.server.course_detail.OverviewMedia
import com.joshtalks.joshskills.repository.server.course_detail.OverviewMediaType
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

@Layout(R.layout.course_overview_media_item)
class CourseOverviewMediaViewHolder(
    val overviewMedia: OverviewMedia,
    private val context: Context = AppObjectController.joshApplication
) {

    @com.mindorks.placeholderview.annotations.View(R.id.imageView)
    lateinit var imageView: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.backgroundFade)
    lateinit var backgroundFade: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.playIcon)
    lateinit var playIcon: AppCompatImageView

    @Resolve
    fun onResolved() {
        val imageUrl =
            if (overviewMedia.type == OverviewMediaType.VIDEO) overviewMedia.thumbnailUrl else overviewMedia.url

        imageUrl?.let { setImageView(it, imageView) }

        if (overviewMedia.type == OverviewMediaType.VIDEO) {
            backgroundFade.visibility = View.VISIBLE
            playIcon.visibility = View.VISIBLE
        } else {
            backgroundFade.visibility = View.GONE
            playIcon.visibility = View.GONE
        }
    }

    @Click(R.id.playIcon)
    fun onClick() {
        VideoPlayerActivity.startVideoActivity(
            context,
            null,
            "123",
            overviewMedia.url
        )
    }

    private fun setImageView(url: String, imageView: ImageView) {
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
            .into(imageView)
    }
}
