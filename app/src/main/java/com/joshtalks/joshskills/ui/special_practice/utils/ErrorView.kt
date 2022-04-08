package com.joshtalks.joshskills.ui.special_practice.utils

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R

class ErrorView : ConstraintLayout {
    private lateinit var rootView: ConstraintLayout
    private lateinit var retryBtn: MaterialTextView
    private val valueAnimator = ValueAnimator.ofInt(1, 3).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        duration = 3000
        addUpdateListener {
            retryBtn.text =
                resources.getString(R.string.retry).plus(".".repeat(it.animatedValue as Int))
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.error_view, this)
        rootView = findViewById(R.id.root_view)
        retryBtn = findViewById(R.id.retry_btn)
    }

    fun onSuccess() {
        stopLoadingAnimation()
        visibility = View.GONE
    }

    fun onFailure(callback: ErrorCallback) {
        visibility = View.VISIBLE
        stopLoadingAnimation()
        enableRetryBtn()
        retryBtn.setOnClickListener {
            callback.onRetryButtonClicked()
            disableRetryBtn()
            startLoadingAnimation()
        }
    }

    private fun startLoadingAnimation() {
        valueAnimator.start()
    }

    private fun stopLoadingAnimation() {
        if (valueAnimator.isRunning) {
            valueAnimator.pause()
        }
    }

    fun enableRetryBtn() {
        stopLoadingAnimation()
        retryBtn.isEnabled = true
        retryBtn.isClickable = true
        updateButtonDrawable(retryBtn, R.drawable.blue_btn_grammar_selector)
    }

    fun disableRetryBtn() {
        retryBtn.isEnabled = false
        retryBtn.isClickable = false
        updateButtonDrawable(retryBtn, R.drawable.gray_btn_pressed_state)
    }

    private fun updateButtonDrawable(
        btn: MaterialTextView,
        drawable: Int
    ) {
        btn.setViewBackgroundWithoutResettingPadding(drawable)
    }

    fun View.setViewBackgroundWithoutResettingPadding(@DrawableRes backgroundResId: Int) {
        val paddingBottom = this.paddingBottom
        val paddingStart = ViewCompat.getPaddingStart(this)
        val paddingEnd = ViewCompat.getPaddingEnd(this)
        val paddingTop = this.paddingTop
        setBackgroundResource(backgroundResId)
        ViewCompat.setPaddingRelative(this, paddingStart, paddingTop, paddingEnd, paddingBottom)
    }

    interface ErrorCallback {
        fun onRetryButtonClicked()
    }
}