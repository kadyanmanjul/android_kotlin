package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.SuperstarFeedbackItemViewBinding
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.Feedback
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

class StudentFeedbackListAdapter(
    private var items: List<Feedback>
) :
    RecyclerView.Adapter<StudentFeedbackListAdapter.ViewHolder>() {
    private var context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SuperstarFeedbackItemViewBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position)

    inner class ViewHolder(val binding: SuperstarFeedbackItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(feedback: Feedback, position: Int) {
            with(binding) {
                feedback.photoUrl?.let { binding.userPic.setImage(it) }
                binding.name.text = feedback.name.toString()
                binding.location.text = feedback.place.toString()
                binding.feedback.text = feedback.feedback.toString()
                binding.picContainer.setOnClickListener {
                    feedback.videoUrl?.let {
                        VideoPlayerActivity.startVideoActivity(
                            context,
                            feedback.name,
                            null,
                            feedback.videoUrl
                        )
                    }

                }
            }
        }
    }
}