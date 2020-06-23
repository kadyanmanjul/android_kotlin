package com.joshtalks.joshskills.ui.course_details.extra

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.Transition
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.course_detail.TeacherDetails
import jp.wasabeef.glide.transformations.CropTransformation
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
        teacher_details.text = tgDetails.longDescription
    }

    private fun setupProfilePicture() {
        tgDetails.dpUrl?.run {
            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(84),
                    Utils.dpToPx(84),
                    CropTransformation.CropType.CENTER
                )
            )
            Glide.with(requireContext())
                .load(this)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
                .into(iv_profile_pic)
        }
    }

    private fun setupBackground() {
        tgDetails.bgUrl?.run {
            Glide.with(requireContext()).asBitmap()
                .load(this)
                .override(SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CenterCrop())
                )
                .into(object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        background_view.setImageBitmap(resource)
                        Palette.from(resource).maximumColorCount(24)
                            .generate { palette ->
                                palette?.vibrantSwatch?.run {
                                    background_view.setBackgroundColor(this.rgb)
                                }
                            }
                    }
                })
        }
    }
}