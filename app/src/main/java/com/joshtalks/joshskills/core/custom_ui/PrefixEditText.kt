package com.joshtalks.joshskills.core.custom_ui


import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.joshtalks.joshskills.R
import kotlin.properties.Delegates

class PrefixEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int = 0
) : AppCompatEditText(context, attrs) {


    private val textPaint: TextPaint by lazy {
        TextPaint().apply {
            color = currentHintTextColor
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            this.typeface = typeface
        }
    }

    private val prefixDrawable: PrefixDrawable by lazy { PrefixDrawable(paint) }
    var empty: String = " "

    var prefix: String = ""
        set(value) {
            if (value.isNotBlank()) {
                Log.v(TAG, "prefix: $value")
            }
            field = value
            prefixDrawable.text = value + empty
            updatePrefixDrawable()
        }

    var suffix: String? = null
        set(value) {
            if (!value.isNullOrBlank()) {
                Log.v(TAG, "suffix: $value")
            }
            field = value
            invalidate()
        }

    // These are used to store details obtained from the EditText's rendering process
    private val firstLineBounds = Rect()

    private var isInitialized = false

    init {
        textPaint.textSize = textSize

        updatePrefixDrawable()
        isInitialized = true

        val typedArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PrefixSuffixEditText)
        prefix = typedArray.getString(R.styleable.PrefixSuffixEditText_prefix) ?: ""
        suffix = typedArray.getString(R.styleable.PrefixSuffixEditText_suffix)
        typedArray.recycle()
    }

    /*fun setTypeface(typeface: Typeface) {
        super.setTypeface(typeface)

        if (isInitialized) {
            // this is first called from the constructor when it's not initialized, yet
            textPaint.typeface = typeface
        }

        postInvalidate()
    }*/

    public override fun onDraw(c: Canvas) {
        textPaint.color = currentHintTextColor

        val lineBounds = getLineBounds(0, firstLineBounds)
        prefixDrawable.let {
            it.lineBounds = lineBounds
            it.paint = textPaint
        }

        super.onDraw(c)

        // Now we can calculate what we need!
        val text = text.toString()
        val prefixText: String = prefixDrawable.text
        val textWidth: Float = textPaint.measureText(prefixText + text) + paddingLeft

        suffix?.let {
            // We need to draw this like this because
            // setting a right drawable doesn't work properly and we want this
            // just after the text we are editing (but untouchable)
            val y2 = firstLineBounds.bottom - textPaint.descent()
            c.drawText(it, textWidth, y2, textPaint)
        }
    }

    private fun updatePrefixDrawable() {
        setCompoundDrawablesRelative(prefixDrawable, null, null, null)
    }

    companion object {
        private const val TAG = "PrefixSuffixEditText"
    }
}

internal class PrefixDrawable(
    var paint: Paint,
    var lineBounds: Int = 0
) : Drawable() {

    var text: String by Delegates.observable("") { _, _: String?, _: String? ->
        // Tell it we need to be as big as we want to be!
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        invalidateSelf()
    }


    override fun draw(canvas: Canvas) {
        text.let {
            val y = (lineBounds + canvas.clipBounds.top).toFloat()
            canvas.drawText(it, 0f, y, paint)
        }
    }

    override fun setAlpha(i: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun getIntrinsicHeight(): Int {
        return paint.textSize.toInt()
    }

    override fun getIntrinsicWidth(): Int {
        return paint.measureText(text).toInt()
    }
}