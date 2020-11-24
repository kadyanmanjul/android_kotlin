package com.joshtalks.joshskills.ui.points_history.viewholder

import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.repository.server.points.PointsWorking
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Position
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.layout_point_info_item)
class PointsInfoViewHolder(var points: PointsWorking,val position: Int) {

    @View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @View(R.id.name)
    lateinit var name: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        name.text=points.label
        score.text=":+".plus(points.points)
        if(position.rem(2)==0){
            rootView.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.lightest_blue
                ))
        }else{
            rootView.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.white
                ))
        }
    }
}
