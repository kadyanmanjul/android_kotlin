package com.joshtalks.joshskills.core.custom_ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils

open class JoshButton : MaterialButton {

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    @ColorRes
    val DEFAULT_BACKGROUND_COLOR = R.color.colorAccent

    @ColorRes
    val DEFAULT_TEXT_COLOR = R.color.white

    @ColorRes
    val DEFAULT_RIPPLE_COLOR = R.color.white

    fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        val attr: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.JoshButton, defStyleAttr, 0
        )
        super.setBackgroundTintList(
            ColorStateList.valueOf(
                attr.getColor(
                    R.styleable.JoshButton_android_backgroundTint,
                    ContextCompat.getColor(context, DEFAULT_BACKGROUND_COLOR)
                )
            )
        )
        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_JoshTypography_Heading_H6)
        super.setCornerRadius(
            attr.getDimensionPixelSize(
                R.styleable.GrammarButton_cornerRadius,
                Utils.sdpToPx(R.dimen._36sdp).toInt()
            )
        )
        if (attr.hasValue(R.styleable.JoshButton_android_textSize)) {
            super.setTextSize(
                attr.getDimension(R.styleable.JoshButton_android_textSize, 0f)
            )
        }
        if (attr.hasValue(R.styleable.JoshButton_android_textColor)) {
            super.setTextColor(
                attr.getColor(
                    R.styleable.JoshButton_android_textColor,
                    ContextCompat.getColor(context, DEFAULT_TEXT_COLOR)
                )
            )
        } else {
            super.setTextColor(ContextCompat.getColor(context, DEFAULT_TEXT_COLOR))
        }
        super.setEnabled(attr.getBoolean(R.styleable.JoshButton_android_enabled, true))
        super.setAllCaps(attr.getBoolean(R.styleable.JoshButton_android_textAllCaps, false))
        if (attr.hasValue(R.styleable.JoshButton_android_elevation))
            super.setElevation(attr.getDimension(R.styleable.JoshButton_android_elevation, 0f))
        else
            super.setElevation(0f)
        if (attr.hasValue(R.styleable.JoshButton_android_padding)) {
            super.setPadding(
                attr.getDimensionPixelSize(R.styleable.JoshButton_android_padding, 0),
                attr.getDimensionPixelSize(R.styleable.JoshButton_android_padding, 0),
                attr.getDimensionPixelSize(R.styleable.JoshButton_android_padding, 0),
                attr.getDimensionPixelSize(R.styleable.JoshButton_android_padding, 0),
            )
        } else {
            super.setPadding(
                attr.getDimensionPixelSize(
                    R.styleable.JoshButton_android_paddingLeft,
                    Utils.sdpToPx(R.dimen._16sdp).toInt()
                ),
                attr.getDimensionPixelSize(
                    R.styleable.JoshButton_android_paddingTop,
                    Utils.sdpToPx(R.dimen._14sdp).toInt()
                ),
                attr.getDimensionPixelSize(
                    R.styleable.JoshButton_android_paddingRight,
                    Utils.sdpToPx(R.dimen._16sdp).toInt()
                ),
                attr.getDimensionPixelSize(
                    R.styleable.JoshButton_android_paddingBottom,
                    Utils.sdpToPx(R.dimen._14sdp).toInt()
                ),
            )
        }
        super.setStrokeWidth(attr.getDimension(R.styleable.JoshButton_android_strokeWidth, 0f).toInt())
        if (attr.hasValue(R.styleable.JoshButton_android_strokeColor)) {
            super.setStrokeColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context, attr.getResourceId(R.styleable.JoshButton_android_strokeColor, 0)
                    )
                )
            )
        }
        super.setRippleColorResource(attr.getResourceId(R.styleable.JoshButton_rippleColor, DEFAULT_RIPPLE_COLOR))
        super.setStateListAnimator(null)
        attr.recycle()
    }
}

class JoshLoadingButton(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val button = JoshButton(context, attrs)
    private val progressBar = ProgressBar(context)
    private var text = ""

    init {
        val attributeArray: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.JoshLoadingButton, 0, 0
        )
        text = attributeArray.getString(R.styleable.JoshLoadingButton_android_text) ?: ""
        button.text = text
        button.elevation = 0f
        button.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }
        progressBar.isIndeterminate = true
        try {
            progressBar.indeterminateTintList = ContextCompat.getColorStateList(context, R.color.white)
        } catch (e: Exception) {
        }
        progressBar.elevation = 8f
        progressBar.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        progressBar.visibility = GONE
        super.addView(progressBar)
        super.addView(button)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        button.setOnClickListener(l)
        super.setOnClickListener(l)
    }

    fun setText(text: String) {
        this.text = text
        button.text = text
    }

    fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) VISIBLE else GONE
        progressBar.elevation = 100f
        button.text = if (isLoading) null else text
        button.isEnabled = !isLoading
    }

}


class JoshShimmerButton : ShimmerFrameLayout {
    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private lateinit var button: JoshButton
    private val shimmer =
        Shimmer.AlphaHighlightBuilder()
            .setDuration(1500)
            .setBaseAlpha(1f)
            .setHighlightAlpha(0.6f)
            .setIntensity(0.3f)
            .setTilt(45f)
            .setShape(Shimmer.Shape.LINEAR)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setWidthRatio(0.4f)

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        button = JoshButton(context, attrs)
        val attr: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.JoshShimmerButton, defStyleAttr, 0
        )
        super.setPadding(0, 0, 0, 0)
        button.elevation = 0f
        button.text = attr.getString(R.styleable.JoshShimmerButton_android_text)
        shimmer.setAutoStart(attr.getBoolean(R.styleable.JoshShimmerButton_android_autoStart, true))
        addView(button)
        super.setShimmer(shimmer.build())
        attr.recycle()
    }

    fun setText(text: String) {
        button.text = text
    }

    fun setTextColor(@ColorRes color: Int) {
        button.setTextColor(ContextCompat.getColor(context, color))
    }

    override fun setOnClickListener(l: OnClickListener?) {
        button.setOnClickListener(l)
        super.setOnClickListener(l)
    }
}
