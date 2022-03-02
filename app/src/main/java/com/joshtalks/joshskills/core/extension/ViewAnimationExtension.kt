package com.joshtalks.joshskills.core.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomLayout
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord

fun View.moveViewToScreenCenter(imgGroupChat: AppCompatImageView, txtUnreadCount: TextView) {
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

fun View.transaltionAnimation(fromLocation: IntArray, toLocation: IntArray) {
    this@transaltionAnimation.visibility = View.VISIBLE
    val animSet = AnimationSet(false)
    animSet.fillAfter = false
    animSet.duration = 1000
    //animSet.interpolator = LinearInterpolator()
    val xDiff = (toLocation.get(0) - fromLocation.get(0))
    val translate = TranslateAnimation(
        Animation.ABSOLUTE,  //from xType
        0f,
        Animation.ABSOLUTE,  //to xType
        xDiff.toFloat(),
        Animation.ABSOLUTE,  //from yType
        0f,
        Animation.ABSOLUTE,  //to yType
        toLocation.get(1).toFloat() - fromLocation.get(1).toFloat()
    )

    animSet.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(p0: Animation?) {
            this@transaltionAnimation.visibility = View.GONE
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

fun View.translationAnimationNew(
    toLocation: IntArray,
    customWord: CustomWord,
    optionLayout: CustomLayout?,
    doOnAnimationEnd: (() -> Unit)? = null
) {
    this@translationAnimationNew.visibility = View.VISIBLE
    val slideAnim = AnimatorSet()
    slideAnim.playTogether(
        ObjectAnimator.ofFloat(this, View.X, toLocation[0].toFloat()),
        ObjectAnimator.ofFloat(this, View.Y, toLocation[1].toFloat())
    )
    val slideSet = AnimatorSet()
    slideSet.play(slideAnim)
    slideSet.interpolator = AccelerateDecelerateInterpolator()
    slideSet.duration = 150
    slideSet.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            optionLayout?.let {
                optionLayout.addViewAt(customWord, customWord.choice.sortOrder - 1)
                customWord.updateView(isSelected = true)
            }
            if (doOnAnimationEnd != null)
                doOnAnimationEnd()
            this@translationAnimationNew.visibility = View.GONE
            customWord.visibility = View.VISIBLE
        }
    })
    slideSet.start()
}

fun View.slideUpAnimation(context: Context) {
    val bottomUp = AnimationUtils.loadAnimation(
        context,
        R.anim.slide_up_dialog
    )

    this.startAnimation(bottomUp)
    this.setVisibility(View.VISIBLE)
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
