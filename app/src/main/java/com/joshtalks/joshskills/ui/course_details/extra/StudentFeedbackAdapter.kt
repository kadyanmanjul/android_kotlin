package com.joshtalks.joshskills.ui.course_details.extra

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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.VERSION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.isValidContextForGlide
import com.joshtalks.joshskills.databinding.LayoutListitemStoryBinding
import com.joshtalks.joshskills.repository.server.course_detail.Feedback
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

class StudentFeedbackAdapter(val feedbacks: List<Feedback>) : RecyclerView.Adapter<StudentFeedbackAdapter.FeedbackViewHolder>() {

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
        fun bind(feedback: Feedback) {
            item.name.text = feedback.name
            item.profession.text = feedback.shortDescription
            feedback.thumbnailUrl?.let {
                setDefaultImageView(item.imageCircle, feedback.thumbnailUrl)
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