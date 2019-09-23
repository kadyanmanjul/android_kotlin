package com.joshtalks.joshskills.core.custom_ui


import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.joshtalks.joshskills.R

class PrefixEditText : AppCompatEditText {

    private var mOriginalLeftPadding = -1f
    private var mPrefix: String? = null

    var prefix: String?
        get() = mPrefix
        set(prefix) {
            mPrefix = prefix
            invalidate()
        }

    val completedText: String
        get() = mPrefix!! + getText().toString()

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.EditText)
            mPrefix = typedArray.getString(R.styleable.EditText_prefix)
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mPrefix != null) {
            calculatePrefix()
            canvas.drawText(mPrefix!!, mOriginalLeftPadding,
                getLineBounds(0, null).toFloat(), getPaint())
        }
    }

    private fun calculatePrefix() {
        if (mOriginalLeftPadding == -1f) {
            val widths = FloatArray(mPrefix!!.length)
            paint.getTextWidths(mPrefix, widths)
            var textWidth = 0f
            for (w in widths) {
                textWidth += w
            }
            mOriginalLeftPadding = compoundPaddingLeft.toFloat()
            setPadding(
                (textWidth + mOriginalLeftPadding).toInt(),
                paddingRight, getPaddingTop(),
                paddingBottom
            )
        }
    }
}