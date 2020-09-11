package com.joshtalks.joshskills.ui.voip.extra

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R


class FullScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_dialog_calling_start_mediator)
    }

    companion object {
        fun getPendingIntent(context: Context, id: Int): PendingIntent {
            val fullScreenIntent = Intent(context, FullScreenActivity::class.java)
            fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fullScreenIntent.putExtra(NfcAdapter.EXTRA_ID, id)
            return PendingIntent.getActivity(
                context, 22, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

}