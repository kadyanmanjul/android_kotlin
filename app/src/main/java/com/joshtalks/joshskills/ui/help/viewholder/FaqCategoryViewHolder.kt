package com.joshtalks.joshskills.ui.help.viewholder

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CategorySelectEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LandingPageCategorySelectEventBus
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.faq_category_item_layout)
class FaqCategoryViewHolder(
    val listFAQCategory: List<FAQCategory>,
    var faqCategory: FAQCategory,
    val position: Int
) {

    @View(R.id.iv_category_icon)
    lateinit var categoryIconIV: AppCompatImageView

    @View(R.id.tv_category_name)
    lateinit var categoryNameTV: AppCompatTextView

    @View(R.id.root_view)
    lateinit var cardView: MaterialCardView

    @Resolve
    fun onViewInflated() {
        categoryNameTV.text = faqCategory.categoryName
        GlideToVectorYou
            .init()
            .with(AppObjectController.joshApplication)
            .requestBuilder
            .load(faqCategory.iconUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().centerCrop())
            .into(categoryIconIV)
        if (position != -1)
            setCardDefaultTint()
    }

    private fun setCardDefaultTint() {
        if (position != 1)
            cardView.strokeColor = ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.white,
                null
            )
        else cardView.strokeColor = ResourcesCompat.getColor(
            AppObjectController.joshApplication.resources,
            R.color.button_primary_color,
            null
        )
        cardView.setCardBackgroundColor(
            ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.white,
                null
            )
        )
        categoryIconIV.setColorFilter(
            ResourcesCompat.getColor(
                AppObjectController.joshApplication.resources,
                R.color.transparent,
                null
            )
        )
    }

    @Click(R.id.root_view)
    fun onClick() {
        if (position != -1)
            RxBus2.publish(LandingPageCategorySelectEventBus(position, faqCategory.categoryName))
        else RxBus2.publish(CategorySelectEventBus(listFAQCategory, faqCategory))
    }
}
