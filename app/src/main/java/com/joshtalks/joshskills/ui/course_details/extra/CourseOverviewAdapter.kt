package com.joshtalks.joshskills.ui.course_details.extra

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.CourseOverviewMediaItemBinding
import com.joshtalks.joshskills.repository.server.course_detail.OverviewMedia
import com.joshtalks.joshskills.repository.server.course_detail.OverviewMediaType
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class CourseOverviewAdapter(val listData: List<OverviewMedia>) :
    RecyclerView.Adapter<CourseOverviewAdapter.CourseOverviewMediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseOverviewMediaViewHolder {
        val binding = CourseOverviewMediaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CourseOverviewMediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseOverviewMediaViewHolder, position: Int) {
        holder.bind(listData[position])
        holder.item.playIcon.setOnClickListener {
            VideoPlayerActivity.startVideoActivity(
                AppObjectController.joshApplication,
                null,
                listData[position].video?.id,
                listData[position].video?.video_url
            )
        }
    }

    override fun getItemCount() = listData.size

    class CourseOverviewMediaViewHolder(val item: CourseOverviewMediaItemBinding) : RecyclerView.ViewHolder(item.root) {
        fun bind(overviewMedia: OverviewMedia) {
            if (overviewMedia.type == OverviewMediaType.IMAGE) {
                overviewMedia.url?.run {
                    setImageView(this, item.imageView)
                }
            } else {
                if (overviewMedia.thumbnailUrl.isNullOrEmpty()) {
                    overviewMedia.video?.video_image_url?.run {
                        setImageView(this, item.imageView)
                    }
                } else {
                    overviewMedia.thumbnailUrl.run {
                        setImageView(this, item.imageView)
                    }
                }
            }
            if (overviewMedia.type == OverviewMediaType.VIDEO) {
                item.backgroundFade.visibility = View.VISIBLE
                item.playIcon.visibility = View.VISIBLE
            } else {
                item.backgroundFade.visibility = View.GONE
                item.playIcon.visibility = View.GONE
            }
        }

        private fun setImageView(url: String, imageView: ImageView) {
            val multi = MultiTransformation(
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )

            Glide.with(itemView)
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

}