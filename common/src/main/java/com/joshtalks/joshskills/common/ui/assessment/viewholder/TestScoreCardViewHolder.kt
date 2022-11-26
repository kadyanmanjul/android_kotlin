package com.joshtalks.joshskills.common.ui.assessment.viewholder

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
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
import com.joshtalks.joshskills.common.repository.local.model.assessment.Assessment
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class TestScoreCardViewHolder(
    var assessment: Assessment) {

    
    lateinit var welcomeMsg: AppCompatTextView

    
    lateinit var score: AppCompatTextView

    
    lateinit var extraText: JoshTextView

    
    lateinit var imageView: AppCompatImageView

    @Resolve
    fun onViewInflated() {
        initView()
    }

    private fun initView() {
        welcomeMsg.text = assessment.text1
        score.text = assessment.scoreText
        extraText.text = assessment.text2
        setImageView(assessment.iconUrl)
    }

    private fun setImageView(url: String?) {
        if (url.isNullOrBlank())
            return
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(4),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(AppObjectController.joshApplication)
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
