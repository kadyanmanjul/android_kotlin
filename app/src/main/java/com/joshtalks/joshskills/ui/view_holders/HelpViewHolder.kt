package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.help_view_layout)
class HelpViewHolder(var helpModel: TypeOfHelpModel) :
    BaseCell() {

    @View(R.id.iv_category_icon)
    lateinit var categoryIconIV: AppCompatImageView

    @View(R.id.tv_category_name)
    lateinit var categoryNameTV: AppCompatTextView


    @Resolve
    fun onViewInflated() {
        setImageInImageView(categoryIconIV, helpModel.iconUrl)
        categoryNameTV.text = helpModel.categoryName
    }

    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(HelpRequestEventBus(helpModel))
    }

}



