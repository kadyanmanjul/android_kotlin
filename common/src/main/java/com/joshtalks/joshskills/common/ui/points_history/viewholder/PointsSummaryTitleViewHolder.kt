package com.joshtalks.joshskills.common.ui.points_history.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.core.textColorSet
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.*
import java.math.RoundingMode
import kotlin.math.ln
import kotlin.math.pow

@Parent
@SingleTop

class PointsSummaryTitleViewHolder(
    var date: String,
    var point: Int,
    val awardIconList: List<String>,
    var index: Int
) {


    
    lateinit var rootView: ConstraintLayout

    
    lateinit var name: AppCompatTextView

    
    lateinit var score: AppCompatTextView

    
    lateinit var toggleView: AppCompatImageView

    
    lateinit var iconLayout: LinearLayout

    var isExpanded = false

    private val drawableDown: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_down_32,
            null
        )
    }
    val drawableUp: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_blue_arrow_up_32,
            null
        )
    }

    val drawable = ContextCompat.getDrawable(
        AppObjectController.joshApplication,
        R.drawable.rectangle_with_blue_bound_stroke_corner_8dp
    )
    val drawableSqaure = ContextCompat.getDrawable(
        AppObjectController.joshApplication,
        R.drawable.rectangle_top_rounded_corner_8dp
    )

    @Resolve
    fun onResolved() {
        name.text = date
        name.textColorSet(R.color.text_default)
        rootView.background = drawable
        toggleView.setImageDrawable(drawableDown)
        score.text =getFormattedNumber(point.toLong())
        if (isExpanded) {
            rootView.background = drawableSqaure
            toggleView.setImageDrawable(drawableUp)
            name.textColorSet(R.color.primary_500)
        }
        if (awardIconList.isNullOrEmpty()) {
            iconLayout.visibility = android.view.View.GONE
        } else {
            iconLayout.visibility = android.view.View.VISIBLE
            iconLayout.removeAllViews()
            awardIconList.forEach { iconUrl ->
                val view = addLinerLayout(iconUrl)
                if (view != null) {
                    iconLayout.addView(view)
                }
            }

        }
    }
    fun getFormattedNumber(point : Long): String {
        return when{
            point >= 10000000 -> (point / 10000000.0).toBigDecimal().setScale(2, RoundingMode.UP).toString() + "Cr"
            point >= 100000 ->  (point / 100000.0).toBigDecimal().setScale(2, RoundingMode.UP).toString() + "L"
            point >= 1000 -> (point/1000.0).toBigDecimal().setScale(2, RoundingMode.UP).toString() + "K"
            else -> point.toString()
        }
    }
    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(iconUrl: String?): android.view.View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_icon_item, rootView, false)
        val image = view.findViewById(R.id.image_iv) as ImageView

        if (iconUrl.isNullOrBlank()) {
            return null
        } else {
            image.setImage(iconUrl,AppObjectController.joshApplication)
        }
        return view
    }

    @Expand
    fun onExpand() {
        isExpanded = true
        rootView.background = drawableSqaure
        toggleView.setImageDrawable(drawableUp)
        name.textColorSet(R.color.primary_500)
    }

    @Collapse
    fun onCollapse() {
        isExpanded = false
        rootView.background = drawable
        name.textColorSet(R.color.text_default)
        toggleView.setImageDrawable(drawableDown)
    }
}
