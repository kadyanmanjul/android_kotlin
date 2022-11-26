package com.joshtalks.joshskills.common.ui.course_details.viewholder

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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.TeacherDetails
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


class TeacherDetailsViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    val data: TeacherDetails,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var txtTeacherName: JoshTextView

    
    lateinit var txtDesignation: JoshTextView

    
    lateinit var txtDescription: JoshTextView

    
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

    
    fun onClick() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(data)
    }

    private fun setImageView(url: String, imageView: ImageView) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(16),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(imageView)
    }
}
