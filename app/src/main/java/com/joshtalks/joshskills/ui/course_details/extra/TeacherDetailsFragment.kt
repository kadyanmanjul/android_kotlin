package com.joshtalks.joshskills.ui.course_details.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.course_detail.TeacherDetails
import kotlinx.android.synthetic.main.fragment_teacher_details.*

const val TEACHER_DETAIL_SOURCE = "teacher_detail_source"

class TeacherDetailsFragment : DialogFragment() {

    companion object {
        fun newInstance(teacherDetails: TeacherDetails) =
            TeacherDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(TEACHER_DETAIL_SOURCE, teacherDetails)
                }
            }
    }

    private lateinit var tgDetails: TeacherDetails

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tgDetails = it.getParcelable<TeacherDetails>(TEACHER_DETAIL_SOURCE) as TeacherDetails
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
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
        return inflater.inflate(R.layout.fragment_teacher_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackground()
        setupProfilePicture()
        teacher_name.text = tgDetails.name
        teacher_details.text =
            HtmlCompat.fromHtml(tgDetails.longDescription, HtmlCompat.FROM_HTML_MODE_LEGACY)
        iv_cross.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun setupProfilePicture() {
        tgDetails.dpUrl?.run {
            Glide.with(requireContext())
                .load(this)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.circleCropTransform())
                .into(iv_profile_pic)
        }
    }

    private fun setupBackground() {
        tgDetails.bgUrl?.run {
            Glide.with(requireContext())
                .load(this)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CenterCrop())
                )
                .into(background_view)
        }
    }
}