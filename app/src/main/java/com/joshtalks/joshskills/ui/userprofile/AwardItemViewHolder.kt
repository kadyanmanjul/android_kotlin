package com.joshtalks.joshskills.ui.userprofile

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.repository.server.Award
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.award_item_view_holder)
class AwardItemViewHolder(var award: Award, var context: Context) {

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.date)
    lateinit var date: AppCompatTextView

    @View(R.id.image)
    lateinit var image: ImageView

    @Resolve
    fun onViewInflated() {
        initView()
    }

    private fun initView() {
        title.text = award.awardText
        date.text = award.dateText
        award.imageUrl?.let {
            image.setImage(it, context)
        }
    }
}

