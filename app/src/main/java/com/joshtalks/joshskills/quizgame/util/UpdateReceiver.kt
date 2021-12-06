package com.joshtalks.joshskills.quizgame.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.joshtalks.joshskills.core.showToast

class UpdateReceiver : BroadcastReceiver(){
    val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

    override fun onReceive(context: Context, intent: Intent) {
        val action: String? = intent.getAction()
        if (action.equals(CONNECTIVITY_CHANGE, ignoreCase = true)) {
            if (!isNetworkAvailable(context)) {
                showToast("The Internet connection appears to be offline")
            }
        }
    }

    companion object{
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo: NetworkInfo? = connectivityManager.getActiveNetworkInfo()
            return activeNetworkInfo != null && activeNetworkInfo.isConnected()
        }
    }
}