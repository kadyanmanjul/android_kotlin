package com.joshtalks.joshskills.core.custom_ui


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R

/**
 * Get the data value from a TypedValue.
 * Ideally you should pass a reused TypedValue instance.
 */
fun TypedArray.getData(typedValue: TypedValue, styleId: Int) =
    if (getValue(styleId, typedValue)) typedValue else null

/**
 * Ripple Pulse RelativeLayout
 */
class RipplePulseRelativeLayout : RelativeLayout {

    /**
     * Constant values to select the ripple's type
     */
    companion object {
        private const val STROKE = 1
        private const val FILL = 0
    }

    /**
     * Object that paints either a circle with a stroke or with a fill color
     */
    private lateinit var ripplePaint: Paint

    /**
     * The bounds of the Paint object
     */
    private lateinit var rippleBounds: RectF

    /**
     * Animator set to apply and handle the different effects
     */
    private lateinit var animatorSet: AnimatorSet

    /**
     * The updated radius
     */
    private var radius = 0F

    /**
     * The ripple Color
     */
    private var _rippleColor = 0
    var rippleColor
        get() = _rippleColor
        set(value) {
            _rippleColor = value
            invalidatePaint()
        }

    /**
     * The pulse Type
     */
    private var _pulseType = STROKE
    var pulseType
        get() = _pulseType
        set(value) {
            _pulseType = value
            invalidatePaint()
        }

    /**
     * The ripple Stroke Width
     */
    private var _rippleStrokeWidth = 0F
    var rippleStrokeWidth
        get() = _rippleStrokeWidth
        set(value) {
            _rippleStrokeWidth = value
            invalidatePaint()
        }

    /**
     * The pulse animation Duration
     */
    private var _pulseDuration = 0
    var pulseDuration
        get() = _pulseDuration
        set(value) {
            _pulseDuration = value
            restartPulse()
        }

    /**
     * The pulse animation startDelay
     */
    private var _startDelay = 0
    var startDelay
        get() = _startDelay
        set(value) {
            _startDelay = value
            restartPulse()
        }

    /**
     * The pulse animation endDelay
     */
    private var _endDelay = 0
    var endDelay
        get() = _endDelay
        set(value) {
            _endDelay = value
            restartPulse()
        }

    /**
     * The ripple Start Radius
     */
    private var _rippleStartRadiusPercent = 0F
    var rippleStartRadiusPercent
        get() = _rippleStartRadiusPercent
        set(value) {
            _rippleStartRadiusPercent = value
            restartPulse()
        }

    /**
     * The ripple End Radius
     */
    private var _rippleEndRadiusPercent = 150F
    var rippleEndRadiusPercent
        get() = _rippleEndRadiusPercent
        set(value) {
            _rippleEndRadiusPercent = value
            restartPulse()
        }

    /**
     * The Interpolator to use on for the pulse
     */
    private var _pulseInterpolator = android.R.anim.decelerate_interpolator
    var pulseInterpolator
        get() = _pulseInterpolator
        set(value) {
            _pulseInterpolator = value
            restartPulse()
        }

    /**
     * Shows a preview on the editor
     */
    private var _showPreview = false
    var showPreview
        get() = _showPreview
        set(value) {
            _showPreview = value
            restartPulse()
        }

    // Avoid the use of @JvmOverloads due to some issue that might happen using Instant run
    // https://antonioleiva.com/custom-views-android-kotlin/

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        attrs?.let { initAttributes(it, defStyle) }
    }

    init {
        _rippleColor = ContextCompat.getColor(context, R.color.green)
        _pulseDuration = resources.getInteger(android.R.integer.config_longAnimTime)
        _rippleStrokeWidth = resources.getDimension(R.dimen.dp2)
        // This option is needed to get onDraw called, otherwise we would need to re-measure the bounds of this View
        setWillNotDraw(false)
    }

    private fun initAttributes(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.RipplePulseRelativeLayout,
            defStyle,
            0
        )

        val typedValue = TypedValue()

        _rippleColor =
            a.getColor(R.styleable.RipplePulseRelativeLayout_pulse_layout_RippleColor, rippleColor)

        _rippleStrokeWidth = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_RippleStrokeWidth
        )?.float ?: rippleStrokeWidth

        _pulseDuration = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_PulseDuration
        )?.data ?: pulseDuration

        _startDelay = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_StartDelay
        )?.data ?: startDelay

        _endDelay = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_EndDelay
        )?.data ?: endDelay

        _pulseType = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_PulseType
        )?.data ?: pulseType

        _rippleStartRadiusPercent = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_RippleStartRadiusPercent
        )?.float
            ?: rippleStartRadiusPercent

        _rippleEndRadiusPercent = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_RippleEndRadiusPercent
        )?.float
            ?: rippleEndRadiusPercent

        _pulseInterpolator = a.getData(
            typedValue,
            R.styleable.RipplePulseRelativeLayout_pulse_layout_PulseInterpolator
        )?.resourceId ?: pulseInterpolator

        // TypedValue does not handle properly Boolean values while on inEditMode (Preview window)
        // so we have to get the value from the TypedArray
        if (isInEditMode) _showPreview =
            a.getBoolean(
                R.styleable.RipplePulseRelativeLayout_pulse_layout_ShowPreview,
                showPreview
            )

        a.recycle()

        // Initialize default objects
        ripplePaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
        }
        rippleBounds = RectF()
        animatorSet = AnimatorSet()

        // Initialize Paint
        invalidatePaint()
    }

    private fun restartPulse() {
        stopPulse()
        startPulse()
    }

    fun startPulse() {
        if (!animatorSet.isRunning) {
            startAnimator()
        }
    }

    fun stopPulse() {
        animatorSet.removeAllListeners()
        animatorSet.cancel()
        invalidate()
    }

    fun isAnimationRunning() = animatorSet.isRunning

    private fun startAnimator() {
        val scale = ValueAnimator.ofFloat(rippleStartRadiusPercent, rippleEndRadiusPercent).apply {
            addUpdateListener {
                radius = it.animatedValue as Float
                if (radius > 0) {
                    invalidate(rippleBounds, radius)
                    invalidate()
                }
            }
        }

        val alpha = ValueAnimator.ofInt(255, 0).apply {
            addUpdateListener {
                val alpha = it.animatedValue as Int
                ripplePaint.alpha = alpha
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    ripplePaint.alpha = 255
                }
            })
        }

        animatorSet.apply {
            duration = pulseDuration.toLong()
            startDelay = this@RipplePulseRelativeLayout.startDelay.toLong()
            interpolator = AnimationUtils.loadInterpolator(context, pulseInterpolator)!!
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    postDelayed({
                        if (!animatorSet.isRunning) {
                            invalidate(rippleBounds, rippleStartRadiusPercent)
                            animatorSet.start()
                        }
                    }, endDelay.toLong())
                }
            })
            playTogether(scale, alpha)
        }
        animatorSet.start()
    }

    private fun invalidatePaint() {
        ripplePaint.apply {
            color = rippleColor
            strokeWidth = if (pulseType == STROKE) rippleStrokeWidth else 0F
            style = if (pulseType == FILL) Paint.Style.FILL else Paint.Style.STROKE
        }
    }

    private fun invalidate(bounds: RectF, percent: Float) {
        val halfWidth = (width / 2)
        val halfHeight = (height / 2)
        bounds.apply {
            left = halfWidth - (halfWidth * (percent / 100.0f))
            top = halfHeight - (halfHeight * (percent / 100.0f))
            right = halfWidth + (halfWidth * (percent / 100.0f))
            bottom = halfHeight + (halfHeight * (percent / 100.0f))
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulse()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (isInEditMode && showPreview) {
            invalidate(rippleBounds, rippleStartRadiusPercent)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Makes immediate parent not to clip its children on their bounds
        (parent as? ViewGroup)?.apply {
            clipChildren = false
            if (!isInLayout) requestLayout()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (animatorSet.isRunning || isInEditMode) {

            drawPulse(canvas, rippleBounds, ripplePaint)

            // Shows a demo aspect pulse's progress within the Preview window
            if (isInEditMode && showPreview) {
                for (idx in rippleStartRadiusPercent.toInt()..rippleEndRadiusPercent.toInt() step 50) {
                    val paint =
                        Paint(ripplePaint).apply { alpha = if (pulseType == FILL) 50 else 100 }
                    val bounds = RectF(rippleBounds).apply { invalidate(this, idx.toFloat()) }
                    drawPulse(canvas, bounds, paint)
                }
            }
        }
    }

    private fun drawPulse(canvas: Canvas?, bounds: RectF, paint: Paint) {
        canvas?.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, paint)
    }
}