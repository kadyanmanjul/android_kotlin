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
import com.joshtalks.joshskills.repository.server.assessment.AssessmentMediaType
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.repository.server.assessment.ReviseConceptResponse

class ReviseConceptFragment : Fragment() {
    private lateinit var binding: FragmentReviseConceptBinding
    private lateinit var revise_concept: ReviseConceptResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            revise_concept = it.getParcelable(REVISE_CONCEPT_RESPONSE)!!
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
        binding.textMessageTitle.text = revise_concept.title
        binding.heading.text = revise_concept.heading
        binding.description.text = revise_concept.description
        if (revise_concept.mediaType == AssessmentMediaType.IMAGE)
            setDefaultImageView(binding.video, revise_concept.mediaUrl)
        binding.video.setOnClickListener {
            //TODO play video code
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
        requireActivity().finish()
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
