package com.joshtalks.joshskills.explore.course_details.viewholder

import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.isValidContextForGlide

abstract class DetailsBaseViewHolder(itemView: ViewDataBinding) : RecyclerView.ViewHolder(itemView.root) {
    open fun bindData(sequence: Int, cardData: JsonObject) {}

    fun getAppContext() = AppObjectController.joshApplication

    fun setDefaultImageView(iv: ImageView, url: String) {
        if (isValidContextForGlide(getAppContext())) {
            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(iv)
        }
    }
}