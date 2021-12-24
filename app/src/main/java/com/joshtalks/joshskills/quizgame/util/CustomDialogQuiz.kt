package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R


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
            positiveBtnAction()
            dialog.dismiss()
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

//    private fun openChoiceScreen(){
//        val fm = activity.supportFragmentManager
//        fm?.beginTransaction()
//            ?.replace(R.id.container,
//                ChoiceFragnment.newInstance(),"TeamMate")
//            ?.remove(this)
//            ?.commit()
//        fm?.popBackStack()
//    }
}