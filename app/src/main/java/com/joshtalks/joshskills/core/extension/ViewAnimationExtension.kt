package com.joshtalks.joshskills.core.extension

import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.core.Utils

fun View.moveViewToScreenCenter(imgGroupChat: AppCompatImageView, txtUnreadCount: TextView) {
    val fromLocation = IntArray(2)
    this.getLocationOnScreen(fromLocation)
    val animSet = AnimationSet(false)
    animSet.fillAfter = false
    animSet.duration = 700
    //animSet.interpolator = LinearInterpolator()
    val translate = TranslateAnimation(
        Animation.ABSOLUTE,  //from xType
        0f,
        Animation.ABSOLUTE,  //to xType
        0f,
        Animation.ABSOLUTE,  //from yType
        0f,
        Animation.ABSOLUTE,  //to yType
        -this.height.times(2).toFloat()
    )

    val fade = AlphaAnimation(1f, 0f)
    val scaleAnimation = ScaleAnimation(
        1f,
        0f,
        1f,
        0f,
        Animation.RELATIVE_TO_SELF,
        1f,
        Animation.RELATIVE_TO_SELF,
        1f
    )
    animSet.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(p0: Animation?) {
            this@moveViewToScreenCenter.visibility = View.GONE
            imgGroupChat.shiftGroupChatIconUp(txtUnreadCount)
        }

        override fun onAnimationStart(p0: Animation?) {

        }

        override fun onAnimationRepeat(p0: Animation?) {
        }
    })
    translate.interpolator = LinearInterpolator()
    scaleAnimation.interpolator = LinearInterpolator()
    animSet.addAnimation(scaleAnimation)
    animSet.addAnimation(translate)
    animSet.addAnimation(fade)
    this.startAnimation(animSet)
}

fun View.slideOutAnimation(imgGroupChat: AppCompatImageView, txtUnreadCount: TextView) {
    val fromLocattion = IntArray(2)
    this.getLocationOnScreen(fromLocattion)
    val animSet = AnimationSet(false)
    animSet.fillAfter = false
    animSet.duration = 700
    animSet.interpolator = LinearInterpolator()
    val translate = TranslateAnimation(
        Animation.ABSOLUTE,  //from xType
        0f,
        Animation.ABSOLUTE,  //to xType
        0f,
        Animation.ABSOLUTE,  //from yType
        0f,
        Animation.ABSOLUTE,  //to yType
        -this.height.times(2).toFloat()
    )

    val fade = AlphaAnimation(1f, 0f)
    val scaleAnimation = ScaleAnimation(
        1f,
        0f,
        1f,
        0f,
        Animation.RELATIVE_TO_SELF,
        1f,
        Animation.RELATIVE_TO_SELF,
        0f
    )
    animSet.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(p0: Animation?) {
            this@slideOutAnimation.visibility = View.GONE
            imgGroupChat.shiftGroupChatIconUp(txtUnreadCount)
        }

        override fun onAnimationStart(p0: Animation?) {

        }

        override fun onAnimationRepeat(p0: Animation?) {
        }
    })
    //translate.interpolator = LinearInterpolator()
    scaleAnimation.interpolator = LinearInterpolator()
    animSet.addAnimation(scaleAnimation)
    //animSet.addAnimation(translate)
    //animSet.addAnimation(fade)
    this.startAnimation(animSet)
}

fun View.slideInAnimation() {
    if (this.visibility != View.VISIBLE) {
        this@slideInAnimation.visibility = View.VISIBLE
        val fromLocattion = IntArray(2)
        this.getLocationOnScreen(fromLocattion)
        val animSet = AnimationSet(false)
        animSet.fillAfter = true
        animSet.duration = 250
        //animSet.interpolator = LinearInterpolator()
        val translate = TranslateAnimation(
            Animation.ABSOLUTE,  //from xType
            0f,
            Animation.ABSOLUTE,  //to xType
            0f,
            Animation.ABSOLUTE,  //from yType
            -this.height.toFloat(),
            Animation.ABSOLUTE,  //to yType
            0f
        )

        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(p0: Animation?) {
            }

            override fun onAnimationStart(p0: Animation?) {

            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        })
        translate.interpolator = LinearInterpolator()
        animSet.addAnimation(translate)
        this.startAnimation(animSet)
    }
}

fun AppCompatImageView.shiftGroupChatIconUp(txtUnreadCount: TextView) {
    val paramsChat: ViewGroup.MarginLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
    paramsChat.topMargin = Utils.dpToPx(20)
    this.layoutParams = paramsChat
    val paramsBadge: ViewGroup.MarginLayoutParams =
        txtUnreadCount.layoutParams as ViewGroup.MarginLayoutParams
    paramsBadge.topMargin = Utils.dpToPx(16)
    txtUnreadCount.layoutParams = paramsBadge
}


fun AppCompatImageView.shiftGroupChatIconDown(txtUnreadCount: TextView) {
    val paramsChat: ViewGroup.MarginLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
    paramsChat.topMargin = Utils.dpToPx(72)
    this.layoutParams = paramsChat
    val paramsBadge: ViewGroup.MarginLayoutParams =
        txtUnreadCount.layoutParams as ViewGroup.MarginLayoutParams
    paramsBadge.topMargin = Utils.dpToPx(64)
    txtUnreadCount.layoutParams = paramsBadge
}
