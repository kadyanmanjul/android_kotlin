package com.joshtalks.joshskills.ui.voip.extra

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity


class FullScreenActivity : AppCompatActivity() {

    companion object {
        fun getPendingIntent(context: Context, id: Int): PendingIntent {
            val fullScreenIntent = Intent(context, FullScreenActivity::class.java)
            fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            fullScreenIntent.putExtra(NfcAdapter.EXTRA_ID, id)
            return PendingIntent.getActivity(
                context, 22, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

}
