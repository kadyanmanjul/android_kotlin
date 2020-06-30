package com.joshtalks.joshskills.ui.view_holders

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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

    @View(R.id.imageView4)
    lateinit var itemIcon: ImageView

    @Toggle(R.id.mainView)
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
        logAnalyticsEvent(question)
        itemIcon.setImageDrawable(drawable)
    }

    @Collapse
    fun onCollapse() {
        itemIcon.setImageDrawable(drawable2)
    }

    fun logAnalyticsEvent(selectedCategory: String) {
        AppAnalytics.create(AnalyticsEvent.QNA_QUESTION_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.QNA_CARD_CLICKED.NAME, selectedCategory).push()
    }
}
