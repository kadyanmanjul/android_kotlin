package com.joshtalks.joshskills.common.ui.userprofile

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.ui.userprofile.models.Award
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


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

