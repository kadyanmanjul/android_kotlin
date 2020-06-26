package com.joshtalks.joshskills.ui.view_holders

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.course_detail.TeacherDetails
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

@Layout(R.layout.teacher_details_view_holder)
class TeacherDetailsViewHolder(
    override val sequenceNumber: Int,
    val data: TeacherDetails,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.txtTeacherName)
    lateinit var txtTeacherName: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.txtDesignation)
    lateinit var txtDesignation: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.txtDescription)
    lateinit var txtDescription: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.imgTeacher)
    lateinit var imgTeacher: AppCompatImageView

    @Resolve
    fun onResolved() {
        txtTeacherName.text = data.name
        txtDesignation.text = data.designation
        txtDescription.text = data.shortDescription
        data.dpUrl?.let {
            setImageView(it, imgTeacher)
        }
    }

    @Click(R.id.btn_meet_me)
    fun onClick() {
        // TODO(Mohit) - Show Teacher Details Fragment
    }

    private fun setImageView(url: String, imageView: ImageView) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(context)
            .load(url)
            .centerCrop()
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(imageView)
    }
}
