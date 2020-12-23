package com.joshtalks.joshskills.core.custom_ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.ContentViewCallback
import com.joshtalks.joshskills.R


class PointsSnackbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ContentViewCallback {

    lateinit var tvMsg: TextView
    lateinit var rootView: ConstraintLayout

    init {
        View.inflate(context, R.layout.point_snackbar, this)
        clipToPadding = false
        this.tvMsg = findViewById(R.id.tv_message)
        this.rootView = findViewById(R.id.snack_constraint)

    }


    override fun animateContentIn(delay: Int, duration: Int) {
    }

    override fun animateContentOut(delay: Int, duration: Int) {
    }
}