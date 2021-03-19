package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.SuperstarFeedbackItemViewBinding
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.Feedback
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

const val FEEDBACK_OBJ = "feedback_obj"

class SuperstarItemFragment : Fragment() {

    private lateinit var binding: SuperstarFeedbackItemViewBinding

    companion object {
        fun newInstance(feedback: Feedback) = SuperstarItemFragment().apply {
            arguments = Bundle().apply {
                putParcelable(FEEDBACK_OBJ, feedback)
            }
        }
    }

    private lateinit var feedback: Feedback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            feedback = it.getParcelable<Feedback>(FEEDBACK_OBJ) as Feedback
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.superstar_feedback_item_view,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedback.photoUrl?.let { binding.userPic.setImage(it) }
        binding.name.text = feedback.name.toString()
        binding.location.text = feedback.place.toString()
        binding.feedback.text = feedback.feedback.toString()
        binding.picContainer.setOnClickListener {
            feedback.videoUrl?.let {
                VideoPlayerActivity.startVideoActivity(
                    requireActivity(),
                    feedback.name,
                    null,
                    feedback.videoUrl
                )
            }

        }
    }
}
