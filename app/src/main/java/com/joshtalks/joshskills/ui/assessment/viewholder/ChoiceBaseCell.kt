package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType

abstract class ChoiceBaseCell(
    open val type: ChoiceType,
    open val sequenceNumber: Int,
    open val choiceData: Choice,
    open val context: Context = AppObjectController.joshApplication
) {

    fun setDefaultImageView(imgView: ImageView, url: String) {
        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(imgView)
    }

}
