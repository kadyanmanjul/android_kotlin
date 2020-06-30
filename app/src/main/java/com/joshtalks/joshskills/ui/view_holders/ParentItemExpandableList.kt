package com.joshtalks.joshskills.ui.view_holders

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.Collapse
import com.mindorks.placeholderview.annotations.expand.Expand
import com.mindorks.placeholderview.annotations.expand.Parent
import com.mindorks.placeholderview.annotations.expand.SingleTop
import com.mindorks.placeholderview.annotations.expand.Toggle

@Parent
@SingleTop
@Layout(R.layout.faq_item)
class ParentItemExpandableList(val question: String) {

    @View(R.id.question)
    lateinit var itemNameTxt: TextView

    @Toggle(R.id.imageView4)
    @View(R.id.imageView4)
    lateinit var itemIcon: ImageView

    @View(R.id.mainView)
    lateinit var mainView: ConstraintLayout

    val drawable: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_down_24,
            null
        )
    }
    val drawable2: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_up_24,
            null
        )
    }

    @Resolve
    fun onResolved() {
        itemIcon.setImageDrawable(drawable2)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainView.setBackgroundColor(
                AppObjectController.joshApplication.resources.getColor(
                    R.color.light_blue,
                    null
                )
            )
        itemNameTxt.text = question
    }

    @Expand
    fun onExpand() {
        itemIcon.setImageDrawable(drawable)
    }

    @Collapse
    fun onCollapse() {
        itemIcon.setImageDrawable(drawable2)
    }
}
