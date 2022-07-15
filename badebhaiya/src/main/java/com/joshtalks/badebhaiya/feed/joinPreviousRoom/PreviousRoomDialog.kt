package com.joshtalks.badebhaiya.feed.joinPreviousRoom

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import com.joshtalks.badebhaiya.R

class PreviousRoomDialog(
    context: Context,
    private val roomName: String,
    private val onPositiveButtonClick: () -> Unit,
    private val onNegativeButtonClick: () -> Unit,
) : Dialog(context) {

    override fun onStart() {
        super.onStart()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_previous_room)
        val titleTv = findViewById<TextView>(R.id.title_previous_room)
        titleTv.text = getTitle()

        val joinButton = findViewById<TextView>(R.id.iWillJoin)
        val endButton = findViewById<TextView>(R.id.endButton)

        joinButton.setOnClickListener {
            onPositiveButtonClick.invoke()
            dismiss()
        }

        endButton.setOnClickListener {
            onNegativeButtonClick.invoke()
            dismiss()
        }
    }

    private fun getTitle(): String{
        return context.getString(R.string.your_call_crack_upsc_is_still_live_do_you_want_to_end_that_call, getTheRoomName())
    }

    private fun getTheRoomName(): String{
        return if (roomName.length > 10){
            "${roomName.substring(0..10)}..."
        } else {
            roomName
        }
    }

}