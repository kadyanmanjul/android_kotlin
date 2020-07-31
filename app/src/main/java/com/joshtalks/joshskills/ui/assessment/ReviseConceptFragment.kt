package com.joshtalks.joshskills.ui.assessment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FragmentReviseConceptBinding
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.repository.server.assessment.ReviseConceptResponse
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity


class ReviseConceptFragment : Fragment() {
    private lateinit var binding: FragmentReviseConceptBinding
    private lateinit var reviseConceptResponse: ReviseConcept

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            reviseConceptResponse = it.getParcelable(REVISE_CONCEPT_DETAILS)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_revise_concept, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textMessageTitle.text = reviseConceptResponse.title
        binding.heading.text = reviseConceptResponse.heading
        binding.description.text = reviseConceptResponse.description
        reviseConceptResponse.videoThumbnailUrl?.let {
            binding.videoGroup.visibility = View.VISIBLE
            setDefaultImageView(binding.thumbnailImage, reviseConceptResponse.mediaUrl)
            binding.thumbnailImage.setOnClickListener {
                VideoPlayerActivity.startVideoActivity(
                    requireContext(),
                    reviseConceptResponse.title,
                    null,
                    reviseConceptResponse.mediaUrl
                )
            }
        }

    }

    fun setDefaultImageView(iv: ImageView, url: String) {
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(iv)
    }

    fun dismiss() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    companion object {
        const val REVISE_CONCEPT_RESPONSE = "revise_concept_response"
        const val REVISE_CONCEPT_DETAILS = "revise_concept_details"

        @JvmStatic
        fun newInstance(reviseConcept: ReviseConcept) =
            ReviseConceptFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(REVISE_CONCEPT_DETAILS, reviseConcept)
                }
            }

        @JvmStatic
        fun newInstance(reviseConcept: ReviseConceptResponse) =
            ReviseConceptFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(REVISE_CONCEPT_RESPONSE, reviseConcept)
                }
            }
    }
}
