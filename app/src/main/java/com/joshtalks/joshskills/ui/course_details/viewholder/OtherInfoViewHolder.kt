package com.joshtalks.joshskills.ui.course_details.viewholder

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.OtherInfoViewHolderBinding
import com.joshtalks.joshskills.repository.server.course_detail.OtherInfo

class OtherInfoViewHolder(
    val item: OtherInfoViewHolderBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            OtherInfo::class.java
        )
        if (sequence != -1)
            data?.imgUrl?.let {
                setImageView(data.imgUrl, item.imageView)
            }
        else item.imageView.setImageDrawable(
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
