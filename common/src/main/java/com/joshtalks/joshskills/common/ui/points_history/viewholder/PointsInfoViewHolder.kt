package com.joshtalks.joshskills.common.ui.points_history.viewholder

import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.repository.server.points.PointsWorking
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Position
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


class PointsInfoViewHolder(var points: PointsWorking,val position: Int) {

    lateinit var rootView: ConstraintLayout

    lateinit var name: AppCompatTextView


    lateinit var score: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        name.text=points.label
        score.text=": +".plus(points.points)
        if(position.rem(2)==0){
            rootView.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.surface_information
                ))
        }else{
            rootView.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.pure_white
                ))
        }
    }
}
