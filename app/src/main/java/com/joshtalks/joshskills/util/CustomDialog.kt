package com.joshtalks.joshskills.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import com.joshtalks.joshskills.R

class CustomDialog(
    context: Context,
    val title: String,
    val message: String,
    val buttonText: String = "Okay"
) : Dialog(context) {


    override fun onStart() {
        super.onStart()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog_layout)
        val titleTv = findViewById<TextView>(R.id.title_tv)
        val messageTv = findViewById<TextView>(R.id.message_tv)
        val buttonBt = findViewById<TextView>(R.id.button)

        titleTv.text = title
        messageTv.text = message
        buttonBt.text = buttonText
        buttonBt.setOnClickListener { dismiss() }

    }

}