package com.joshtalks.joshskills.premium.ui.userprofile

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.setImage
import com.joshtalks.joshskills.premium.ui.userprofile.models.Award
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.award_item_view_holder)
class AwardItemViewHolder(var award: Award, var dateText:String?, var context: Context) {

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.date)
    lateinit var date: AppCompatTextView

    @View(R.id.image)
    lateinit var image: ImageView

    @View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @Resolve
    fun onViewInflated() {
        initView()
    }

    private fun initView() {
        title.text = award.awardText
        date.text = dateText
        award.imageUrl?.let {
            image.setImage(it, context)
        }
    }

//    @Click(R.id.root_view)
//    fun onClick() {
//        RxBus2.publish(
//            AwardItemClickedEventBus(award)
//        )
//    }
}

