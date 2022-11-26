package com.joshtalks.joshskills.common.ui.help.viewholder

import android.graphics.drawable.PictureDrawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.CategorySelectEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.LandingPageCategorySelectEventBus
import com.joshtalks.joshskills.common.repository.server.FAQCategory
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

class FaqCategoryViewHolder(
    private val listFAQCategory: List<FAQCategory>,
    private var faqCategory: FAQCategory,
    val position: Int
) {

    
    lateinit var categoryIconIV: AppCompatImageView

    
    lateinit var categoryNameTV: AppCompatTextView

    
    lateinit var cardView: MaterialCardView

    @Resolve
    fun onViewInflated() {
        categoryNameTV.text = faqCategory.categoryName

        if (faqCategory.iconUrl.endsWith(".svg")) {

            val requestBuilder = GlideToVectorYou
                .init()
                .with(AppObjectController.joshApplication)
                .requestBuilder
            requestBuilder.load(faqCategory.iconUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions().centerCrop())
                .listener(object : RequestListener<PictureDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<PictureDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: PictureDrawable?,
                        model: Any?,
                        target: Target<PictureDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let {
                            AppObjectController.uiHandler.post {
                                categoryIconIV.setImageDrawable(it)
                            }
                        }
                        return false
                    }

                }).submit()

        } else {
            categoryIconIV.setImage(faqCategory.iconUrl)
        }
        if (position != -1)
            setCardDefaultTint()
    }

    private fun setCardDefaultTint() {
        if (position != 1) {
            TextViewCompat.setTextAppearance(
                categoryNameTV,
                R.style.TextAppearance_JoshTypography_BodyRegular20
            )
            cardView.strokeColor = ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.pure_white,
                null
            )
        } else {
            TextViewCompat.setTextAppearance(
                categoryNameTV,
                R.style.TextAppearance_JoshTypography_Body_Text_Small_Bold
            )
            cardView.strokeColor = ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.primary_500,
                null
            )
        }
        cardView.setCardBackgroundColor(
            ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.pure_white,
                null
            )
        )
    }

    
    fun onClick() {
        if (position != -1)
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                LandingPageCategorySelectEventBus(
                    position,
                    faqCategory.id,
                    faqCategory.categoryName
                )
            )
        else com.joshtalks.joshskills.common.messaging.RxBus2.publish(CategorySelectEventBus(listFAQCategory, faqCategory))
    }
}
