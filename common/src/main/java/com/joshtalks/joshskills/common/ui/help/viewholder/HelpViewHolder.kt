package com.joshtalks.joshskills.common.ui.help.viewholder

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.SINGLE_SPACE
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.common.repository.server.help.Option
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.help_view_layout)
class HelpViewHolder(var option: Option, var unreadMessages: Int) {

    @View(R.id.iv_category_icon)
    lateinit var categoryIconIV: AppCompatImageView

    @View(R.id.tv_category_name)
    lateinit var categoryNameTV: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        if (unreadMessages <= 1)
            categoryNameTV.text = option.name
        else categoryNameTV.text = option.name.plus(SINGLE_SPACE).plus("(${unreadMessages} Msg)")

        GlideToVectorYou
            .init()
            .with(AppObjectController.joshApplication)
            .requestBuilder
            .load(option.url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().centerCrop())
            .into(categoryIconIV)
    }

    @Click(R.id.root_view)
    fun onClick() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(HelpRequestEventBus(option))
    }

}



