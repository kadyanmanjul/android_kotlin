package com.joshtalks.joshskills.premium.ui.help.viewholder

import android.graphics.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.SINGLE_SPACE
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.premium.repository.server.help.Option
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
            .into(categoryIconIV)
    }

    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(HelpRequestEventBus(option))
    }

}



