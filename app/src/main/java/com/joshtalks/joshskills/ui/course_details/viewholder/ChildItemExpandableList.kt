package com.joshtalks.joshskills.ui.course_details.viewholder

import android.os.Build
import android.view.View.INVISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.Toggle

@Layout(R.layout.faq_item)
class ChildItemExpandableList(private val answer: String) {

    @View(R.id.question)
    lateinit var itemNameTxt: TextView

    @Toggle(R.id.imageView4)
    @View(R.id.imageView4)
    lateinit var itemIcon: ImageView

    @View(R.id.mainView)
    lateinit var mainView: CardView

    @Resolve
    fun onResolved() {
        itemIcon.visibility = INVISIBLE
        itemNameTxt.text = answer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainView.setCardBackgroundColor(
                AppObjectController.joshApplication.resources.getColor(
                    R.color.white,
                    null
                )
            )
    }
}
