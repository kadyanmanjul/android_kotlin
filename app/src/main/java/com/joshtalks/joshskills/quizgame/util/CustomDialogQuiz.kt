package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import com.joshtalks.joshskills.R
import androidx.core.content.ContextCompat.startActivity
import com.joshtalks.joshskills.quizgame.StartActivity


class CustomDialogQuiz(var activity: Activity) : Dialog(activity) {

    override fun onStart() {
        super.onStart()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog)
        val btnNo = findViewById<ImageView>(R.id.btn_no)
        val btnYes = findViewById<ImageView>(R.id.btn_yes)
        val btnCancel = findViewById<ImageView>(R.id.btn_cancel)

        btnYes.setOnClickListener {
                    dismiss()
                    openFavouritePartnerScreen()
                    AudioManagerQuiz.audioRecording.stopPlaying()
        }
        btnNo.setOnClickListener {
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
        show()
    }
    fun openFavouritePartnerScreen(){
        startActivity(context,Intent(context,StartActivity::class.java),Bundle())
    }
}