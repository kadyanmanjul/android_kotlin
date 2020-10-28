package com.joshtalks.joshskills.ui.voip.extra

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.CustomIconRatingBar

class PractiseRatingView : FrameLayout {
    private lateinit var title: AppCompatTextView
    private lateinit var ratingView: CustomIconRatingBar
    private var rating: Int = 1

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.practise_rating_view, this)
        title = findViewById(R.id.tv_title)
        ratingView = findViewById(R.id.rating_bar)
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PractiseRatingView, 0, 0)
        a.getString(R.styleable.PractiseRatingView_rating_title)?.let {
            title.text = it
        }
        ratingView.setStar(1F)
        ratingView.setOnRatingChangeListener(object : CustomIconRatingBar.OnRatingChangeListener {
            override fun onRatingChange(ratingCount: Float) {
                rating = ratingCount.toInt()
            }
        })
    }

    fun getRatingPoint(): Int {
        return rating
    }
}