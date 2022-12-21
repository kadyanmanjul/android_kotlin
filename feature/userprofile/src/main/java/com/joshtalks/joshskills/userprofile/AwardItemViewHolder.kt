package com.joshtalks.joshskills.userprofile

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.ui.userprofile.models.Award
import com.mindorks.placeholderview.annotations.Resolve


class AwardItemViewHolder(var award: Award, var dateText:String?, var context: Context) {

    
    lateinit var title: AppCompatTextView

    
    lateinit var date: AppCompatTextView

    
    lateinit var image: ImageView

    
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

//    
//    fun onClick() {
//        RxBus2.publish(
//            AwardItemClickedEventBus(award)
//        )
//    }
}

