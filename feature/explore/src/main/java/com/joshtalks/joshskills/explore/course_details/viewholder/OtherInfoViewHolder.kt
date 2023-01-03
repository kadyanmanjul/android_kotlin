package com.joshtalks.joshskills.explore.course_details.viewholder

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.course_details.models.OtherInfo
import com.joshtalks.joshskills.explore.databinding.OtherInfoViewHolderBinding

class OtherInfoViewHolder(
    val item: OtherInfoViewHolderBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        if (sequence != -1) {
            val data = AppObjectController.gsonMapperForLocal.fromJson(
                cardData.toString(),
                OtherInfo::class.java
            )
            data?.imgUrl?.let {
                setImageView(data.imgUrl, item.imageView)
            }
        } else item.imageView.setImageDrawable(
            ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.payment_bottom
            )
        )
    }

    private fun setImageView(url: String, imageView: ImageView) {
        Glide.with(imageView)
            .load(url)
            .fitCenter()
            .override(Target.SIZE_ORIGINAL)
            .into(imageView)
    }
}
