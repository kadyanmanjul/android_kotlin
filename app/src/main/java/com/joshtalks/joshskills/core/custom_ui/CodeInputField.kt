package com.joshtalks.joshskills.core.custom_ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import com.joshtalks.joshskills.R


class CodeInputField : LinearLayout {

    var currentPos = 0
    var cellCount = 4
    var list: MutableList<View> = mutableListOf()


    constructor(context: Context) : super(context) {
        init()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?) : this(context)

    @JvmOverloads
    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs)

    @JvmOverloads
    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : this(
        context,
        attrs,
        defStyleAttr
    )

    private fun init() {

        val rootViewOfCodeInput = rootView as LinearLayout
        rootViewOfCodeInput.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rootViewOfCodeInput.orientation = HORIZONTAL

        val attributes = context.obtainStyledAttributes(R.styleable.CodeInputField)
        this.cellCount = attributes.getInt(R.styleable.CodeInputField_cell_count, 4)

        var temp: View
        for (i in 0..cellCount) {
            temp = if (i == 2) {
                getPinCodeView(false)
            } else {
                getPinCodeView(false)
            }
            addView(temp)
            list.add(temp)
        }
        attributes.recycle()

    }

    private fun getPinCodeView(flag: Boolean): View {
        var v = LayoutInflater.from(context).inflate(R.layout.code_input_field_layout, this, false)
        var et:AppCompatEditText=v.findViewById(R.id.et_code)
        et.addTextChangedListener(object :MyTextWatcher(v){
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                var temp:AppCompatEditText=etView.findViewById(R.id.et_code)




            }
        })
        return v

    }


    private fun getDpToPx(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        context.resources.displayMetrics
    ).toInt()


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val redPaint = Paint()
        redPaint.color = Color.RED
        redPaint.strokeWidth = 5f // set stroke so you can actually see the lines
        canvas?.drawLine(0f, 0f, 100f, 100f, redPaint)

    }
}
abstract class MyTextWatcher : TextWatcher {


    var etView:View

    constructor(editText: View) {
        this.etView = editText
    }

    override fun afterTextChanged(s: Editable?) {

    }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}
