package com.joshtalks.joshskills.explore.course_details.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.course_details.models.TeacherDetails

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

    private var tgDetails: TeacherDetails? = null

    private val teacherDetailsTv by lazy {
        view?.findViewById<JoshTextView>(R.id.teacher_details)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tgDetails = it.getParcelable<TeacherDetails>(TEACHER_DETAIL_SOURCE) as TeacherDetails
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
        return inflater.inflate(R.layout.fragment_teacher_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfilePicture()
        view.findViewById<AppCompatTextView>(R.id.teacher_name).text = tgDetails?.name
        teacherDetailsTv?.text =
            HtmlCompat.fromHtml(
                tgDetails?.longDescription ?: EMPTY,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        view.findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.CANCEL).push()
            dismissAllowingStateLoss()
        }

        teacherDetailsTv?.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_PHONE -> Utils.call(requireContext(), matchedText)
                AutoLinkMode.MODE_URL -> Utils.openUrl(matchedText, requireActivity())
                else -> {}
            }
        }
    }

    private fun setupProfilePicture() {
        tgDetails?.dpUrl?.run {
            view?.findViewById<AppCompatImageView>(R.id.iv_profile_pic)?.let {
                Glide.with(requireContext())
                    .load(this)
                    .override(Target.SIZE_ORIGINAL)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    )
                    .apply(RequestOptions.circleCropTransform())
                    .into(it)
            }
        }
    }
}