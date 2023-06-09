package com.joshtalks.joshskills.ui.course_details.viewholder

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.VERSION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
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
class ParentItemExpandableList(val question: String, val testId: Int ,val categoryId: Int) {

    @View(R.id.question)
    lateinit var itemNameTxt: TextView

    @View(R.id.imageView4)
    lateinit var itemIcon: ImageView

    @Toggle(R.id.mainView)
    @View(R.id.mainView)
    lateinit var mainView: CardView

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
        itemIcon.visibility = VISIBLE
        itemIcon.setImageDrawable(drawable2)
        itemNameTxt.text = question
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mainView.setCardBackgroundColor(
                AppObjectController.joshApplication.resources.getColor(
                    R.color.light_blue,
                    null
                )
            )
    }

    @Expand
    fun onExpand() {
        itemIcon.setImageDrawable(drawable)
        logAnalyticsEvent(question)
    }

    @Collapse
    fun onCollapse() {
        itemIcon.setImageDrawable(drawable2)
    }

    fun logAnalyticsEvent(selectedCategory: String) {
        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_QNA_QUESTION_CLICKED)
            .addParam(ParamKeys.TEST_ID,testId)
            .addParam(ParamKeys.QUESTION_NAME,selectedCategory)
            .addParam(ParamKeys.CATEGORY_ID,categoryId)
            .push()

        AppAnalytics.create(AnalyticsEvent.QNA_QUESTION_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.QNA_CARD_CLICKED.NAME, selectedCategory)
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }
}
