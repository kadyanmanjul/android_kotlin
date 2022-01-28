package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar

object UtilsQuiz {
       fun dipDown(targetView: View,activity: Activity) {
        val myAnim = AnimationUtils.loadAnimation(activity, R.anim.bounce_anim)
        val interpolator = MyBounceInterpolator(0.8, 20.0)
        myAnim.interpolator = interpolator
        myAnim.duration = 3000
        myAnim.repeatCount = Animation.INFINITE
        targetView.startAnimation(myAnim)
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
            PointSnackbar.make(view, duration, action_lable)?.show()
    }

    fun getSplitName(name: String?): String {
        return name?.split(" ")?.get(0).toString()
    }
}