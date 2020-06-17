package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.faq_category_item_layout)
class FaqCategoryViewHolder(
    val listTypeOfHelpModel: List<TypeOfHelpModel>,
    var typeOfHelpModel: TypeOfHelpModel
) {

    @View(R.id.iv_category_icon)
    lateinit var categoryIconIV: AppCompatImageView

    @View(R.id.tv_category_name)
    lateinit var categoryNameTV: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        categoryNameTV.text = typeOfHelpModel.categoryName
        GlideToVectorYou
            .init()
            .with(AppObjectController.joshApplication)
            .requestBuilder
            .load(typeOfHelpModel.iconUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().centerCrop())
            .into(categoryIconIV)
    }

    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(listTypeOfHelpModel)
    }

}



