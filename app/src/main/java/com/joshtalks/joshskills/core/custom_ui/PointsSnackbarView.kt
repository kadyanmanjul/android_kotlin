package com.joshtalks.joshskills.core.custom_ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
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
    lateinit var imLeft: ImageView

    init {
        View.inflate(context, R.layout.point_snackbar, this)
        clipToPadding = false
        this.tvMsg = findViewById(R.id.tv_message)
        this.imLeft = findViewById(R.id.iv_image)
    }

    override fun animateContentIn(delay: Int, duration: Int) {
        val scaleX = ObjectAnimator.ofFloat(imLeft, View.SCALE_X, 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(imLeft, View.SCALE_Y, 0f, 1f)
        val animatorSet = AnimatorSet().apply {
            interpolator = OvershootInterpolator()
            setDuration(500)
            playTogether(scaleX, scaleY)
        }
        animatorSet.start()
    }

    override fun animateContentOut(delay: Int, duration: Int) {
    }
}