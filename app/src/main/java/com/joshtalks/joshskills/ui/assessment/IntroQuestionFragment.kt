package com.joshtalks.joshskills.ui.assessment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro
import kotlinx.android.synthetic.main.intro_question_fragment.image_view
import kotlinx.android.synthetic.main.intro_question_fragment.root_view
import kotlinx.android.synthetic.main.intro_question_fragment.tv_description
import kotlinx.android.synthetic.main.intro_question_fragment.tv_tile

const val ASSESSMENT_DETAIL_SOURCE = "teacher_detail_source"

class IntroQuestionFragment : DialogFragment() {

    companion object {
        fun newInstance(assessmentIntro: AssessmentIntro) =
            IntroQuestionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ASSESSMENT_DETAIL_SOURCE, assessmentIntro)
                }
            }
    }

    private lateinit var assessmentIntroObj: AssessmentIntro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assessmentIntroObj =
                it.getParcelable<AssessmentIntro>(ASSESSMENT_DETAIL_SOURCE) as AssessmentIntro
        }
        setStyle(STYLE_NO_FRAME, R.style.FullDialogWithAnimationV2)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.intro_question_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        assessmentIntroObj.imageUrl?.let {
            image_view.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(it)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(image_view)
        }
        if (assessmentIntroObj.imageUrl.isNullOrEmpty()) {
            val params: ViewGroup.MarginLayoutParams =
                tv_description.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = 0
        }
        assessmentIntroObj.title?.run {
            tv_tile.text = this
        }
        assessmentIntroObj.description?.run {
            tv_description.text = this
        }
        root_view.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
