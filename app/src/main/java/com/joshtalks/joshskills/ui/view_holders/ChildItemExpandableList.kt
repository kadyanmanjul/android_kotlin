package com.joshtalks.joshskills.ui.view_holders

import android.os.Build
import android.view.View.GONE
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.Toggle

@Layout(R.layout.faq_item)
class ChildItemExpandableList(val answer: String) {

    @View(R.id.question)
    lateinit var itemNameTxt: TextView

    @Toggle(R.id.imageView4)
    @View(R.id.imageView4)
    lateinit var itemIcon: ImageView

    @View(R.id.mainView)
    lateinit var mainView: ConstraintLayout

    @Resolve
    fun onResolved() {
        itemIcon.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainView.setBackgroundColor(
                AppObjectController.joshApplication.resources.getColor(
                    R.color.white,
                    null
                )
            )
        itemNameTxt.text = answer
    }
}
