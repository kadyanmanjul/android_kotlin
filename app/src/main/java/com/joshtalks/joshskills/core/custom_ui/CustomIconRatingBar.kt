package com.joshtalks.joshskills.core.custom_ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.joshtalks.joshskills.R
import java.math.BigDecimal

class CustomIconRatingBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var mClickable: Boolean
    private var halfstart: Boolean
    private var starCount: Int
    private val starNum: Int
    private var onRatingChangeListener: OnRatingChangeListener? = null
    private var starImageSize: Float
    private var starImageWidth: Float
    private var starImageHeight: Float
    private var starImagePadding: Float
    private var starEmptyDrawable: Drawable?
    private var starFillDrawable: Drawable?
    private var starHalfDrawable: Drawable?
    private var y = 1
    private val isEmpty = true
    fun setStarHalfDrawable(starHalfDrawable: Drawable?) {
        this.starHalfDrawable = starHalfDrawable
    }

    fun setOnRatingChangeListener(onRatingChangeListener: OnRatingChangeListener?) {
        this.onRatingChangeListener = onRatingChangeListener
    }

    fun setmClickable(clickable: Boolean) {
        mClickable = clickable
    }

    fun halfStar(halfstart: Boolean) {
        this.halfstart = halfstart
    }

    fun setStarFillDrawable(starFillDrawable: Drawable?) {
        this.starFillDrawable = starFillDrawable
    }

    fun setStarEmptyDrawable(starEmptyDrawable: Drawable?) {
        this.starEmptyDrawable = starEmptyDrawable
    }

    fun setStarImageSize(starImageSize: Float) {
        this.starImageSize = starImageSize
    }

    fun setStarImageWidth(starImageWidth: Float) {
        this.starImageWidth = starImageWidth
    }

    fun setStarImageHeight(starImageHeight: Float) {
        this.starImageHeight = starImageHeight
    }

    fun setStarCount(starCount: Int) {
        this.starCount = starCount
    }

    fun getStarCount(): Int {
        return starCount
    }

    fun setImagePadding(starImagePadding: Float) {
        this.starImagePadding = starImagePadding
    }

    private fun getStarImageView(context: Context, isEmpty: Boolean): ImageView {
        val imageView = ImageView(context)
        val para = ViewGroup.LayoutParams(
            Math.round(starImageWidth),
            Math.round(starImageHeight)
        )
        imageView.layoutParams = para
        imageView.setPadding(0, 0, Math.round(starImagePadding), 0)
        if (isEmpty) {
            imageView.setImageDrawable(starEmptyDrawable)
        } else {
            imageView.setImageDrawable(starFillDrawable)
        }
        return imageView
    }

    fun setStar(starCount: Float) {
        var starCount = starCount
        val fint = starCount.toInt()
        val b1 = BigDecimal(java.lang.Float.toString(starCount))
        val b2 = BigDecimal(Integer.toString(fint))
        val fPoint = b1.subtract(b2).toFloat()
        starCount = if (fint > this.starCount) this.starCount.toFloat() else fint.toFloat()
        starCount = if (starCount < 0) 0F else starCount

        //drawfullstar
        var i = 0
        while (i < starCount) {
            (getChildAt(i) as ImageView).setImageDrawable(starFillDrawable)
            ++i
        }

        //drawhalfstar
        if (fPoint > 0) {
            (getChildAt(fint) as ImageView).setImageDrawable(starHalfDrawable)

            //drawemptystar
            var i = this.starCount - 1
            while (i >= starCount + 1) {
                (getChildAt(i) as ImageView).setImageDrawable(starEmptyDrawable)
                --i
            }
        } else {
            //drawemptystar
            var i = this.starCount - 1
            while (i >= starCount) {
                (getChildAt(i) as ImageView).setImageDrawable(starEmptyDrawable)
                --i
            }
        }
    }

    /**
     * change start listener
     */
    interface OnRatingChangeListener {
        fun onRatingChange(RatingCount: Float)
    }

    init {
        orientation = HORIZONTAL
        val mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RatingBar)
        starHalfDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starHalf)
        starEmptyDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starEmpty)
        starFillDrawable = mTypedArray.getDrawable(R.styleable.RatingBar_starFill)
        starImageSize = mTypedArray.getDimension(R.styleable.RatingBar_starImageSize, 120f)
        starImageWidth = mTypedArray.getDimension(R.styleable.RatingBar_starImageWidth, 60f)
        starImageHeight = mTypedArray.getDimension(R.styleable.RatingBar_starImageHeight, 120f)
        starImagePadding = mTypedArray.getDimension(R.styleable.RatingBar_starImagePadding, 15f)
        starCount = mTypedArray.getInteger(R.styleable.RatingBar_starCount, 5)
        starNum = mTypedArray.getInteger(R.styleable.RatingBar_starNum, 0)
        mClickable = mTypedArray.getBoolean(R.styleable.RatingBar_clickable, true)
        halfstart = mTypedArray.getBoolean(R.styleable.RatingBar_halfstart, false)
        for (i in 0 until starNum) {
            val imageView = getStarImageView(context, false)
            addView(imageView)
        }
        for (i in 0 until starCount) {
            val imageView = getStarImageView(context, isEmpty)
            imageView.setOnClickListener { v ->
                if (mClickable) {
                    if (halfstart) {
                        //TODO:This is not the best way to solve half a star,
                        //TODO:but That's what I can do,Please let me know if you have a better solution
                        if (y % 2 == 0) {
                            setStar(indexOfChild(v) + 1f)
                        } else {
                            setStar(indexOfChild(v) + 0.5f)
                        }
                        if (onRatingChangeListener != null) {
                            if (y % 2 == 0) {
                                onRatingChangeListener!!.onRatingChange(indexOfChild(v) + 1f)
                                y++
                            } else {
                                onRatingChangeListener!!.onRatingChange(indexOfChild(v) + 0.5f)
                                y++
                            }
                        }
                    } else {
                        setStar(indexOfChild(v) + 1f)
                        if (onRatingChangeListener != null) {
                            onRatingChangeListener!!.onRatingChange(indexOfChild(v) + 1f)
                        }
                    }
                }
            }
            addView(imageView)
        }
        mTypedArray.recycle()
    }
}