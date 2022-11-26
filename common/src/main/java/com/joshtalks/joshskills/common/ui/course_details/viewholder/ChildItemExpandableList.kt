package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.os.Build
import android.view.View.INVISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.Toggle


class ChildItemExpandableList(private val answer: String) {

    lateinit var itemNameTxt: TextView

    lateinit var itemIcon: ImageView

    lateinit var mainView: CardView

    fun onResolved() {
        itemIcon.visibility = INVISIBLE
        itemNameTxt.text = answer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainView.setCardBackgroundColor(
                AppObjectController.joshApplication.resources.getColor(
                    R.color.pure_white,
                    null
                )
            )
    }
}
