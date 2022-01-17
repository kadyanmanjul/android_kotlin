package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics


class CustomDialogQuiz(var activity: Activity) {

    fun showDialog(positiveBtnAction: (() -> Unit)) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
            AudioManagerQuiz.audioRecording.tickPlaying(activity)
            if (Utils.isInternetAvailable()){
                GameAnalytics.push(GameAnalytics.Event.CLICK_ON_EXIT)
                positiveBtnAction()
                dialog.dismiss()
            }else{
                showToast("Seems like your Internet is too slow or not available.")
            }
        }
        noBtn.setOnClickListener {
            AudioManagerQuiz.audioRecording.tickPlaying(activity)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            AudioManagerQuiz.audioRecording.tickPlaying(activity)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun scaleAnimationForNotification(v: View) {
        try {
            val animation = TranslateAnimation(0f, 0f, -100f, 0f)
            animation.duration = 700
            v.startAnimation(animation)
            AudioManagerQuiz().notificationPlaying(activity)
            v.visibility = View.VISIBLE
        } catch (ex: Exception) { }
    }

    fun scaleAnimationForNotificationUpper(v: View) {
        try {
            val anim : Animation = AnimationUtils.loadAnimation(activity,R.anim.abc_slide_out_top)
            anim.duration = 1000
            v.startAnimation(anim)
            v.visibility = View.INVISIBLE
        } catch (ex: Exception) { }
    }
}