package com.joshtalks.joshskills.explore.course_details.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.VERSION
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.isValidContextForGlide
import com.joshtalks.joshskills.common.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.explore.course_details.models.VideoFeedback
import com.joshtalks.joshskills.explore.databinding.LayoutListitemStoryBinding

class StudentFeedbackAdapter(val feedbacks: List<VideoFeedback>) : RecyclerView.Adapter<StudentFeedbackAdapter.FeedbackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = LayoutListitemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(feedbacks[position])
        holder.item.imageCircle.setOnClickListener {
            logAnalyticsEvent(feedbacks[position].name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.item.frameLayout.background.setTint(
                    getAppContext().resources.getColor(
                        R.color.dark_grey,
                        null
                    )
                )
            }
            feedbacks[position].videoUrl?.let {
                VideoPlayerActivity.startVideoActivity(
                    getAppContext(),
                    feedbacks[position].name,
                    null,
                    feedbacks[position].videoUrl
                )
            }
        }
    }

    override fun getItemCount() = feedbacks.size

    fun getAppContext() = AppObjectController.joshApplication

    private fun logAnalyticsEvent(name: String) {
        AppAnalytics.create(AnalyticsEvent.MEET_STUDENT_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.USER_NAME.NAME, name)
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }

    inner class FeedbackViewHolder(val item: LayoutListitemStoryBinding) : RecyclerView.ViewHolder(item.root) {
        fun bind(videoFeedback: VideoFeedback) {
            item.name.text = videoFeedback.name
            item.profession.text = videoFeedback.shortDescription
            videoFeedback.thumbnailUrl?.let {
                setDefaultImageView(item.imageCircle, videoFeedback.thumbnailUrl)
            }
        }

        fun setDefaultImageView(iv: ImageView, url: String) {
            if (isValidContextForGlide(getAppContext())) {
                Glide.with(getAppContext())
                    .load(url)
                    .override(Target.SIZE_ORIGINAL)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    )
                    .into(iv)
            }
        }
    }
}