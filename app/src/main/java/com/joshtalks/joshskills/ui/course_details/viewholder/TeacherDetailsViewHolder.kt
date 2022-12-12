package com.joshtalks.joshskills.ui.course_details.viewholder

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.TeacherDetailsViewHolderBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.course_detail.TeacherDetails
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class TeacherDetailsViewHolder(
    val item: TeacherDetailsViewHolderBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            TeacherDetails::class.java
        )

        item.txtTeacherName.text = data.name
        item.txtDesignation.text = data.designation
        item.txtDescription.text = data.shortDescription
        data.dpUrl?.let {
            setImageView(it, item.imgTeacher)
        }

        item.btnMeetMe.setOnClickListener {
            RxBus2.publish(data)
        }
    }

    private fun setImageView(url: String, imageView: ImageView) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(16),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(itemView)
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
