package com.joshtalks.joshskills.ui.assessment

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
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
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

@Layout(R.layout.test_score_header_item)
class TestScoreCardViewHolder(
    var assessment: Assessment) {

    @View(R.id.welcome_msg)
    lateinit var welcomeMsg: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @View(R.id.extra_text)
    lateinit var extraText: JoshTextView

    @View(R.id.image_view)
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
