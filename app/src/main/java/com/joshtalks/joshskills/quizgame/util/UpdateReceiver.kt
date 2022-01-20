package com.joshtalks.joshskills.quizgame.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast

class UpdateReceiver : BroadcastReceiver(){
    val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

    override fun onReceive(context: Context, intent: Intent) {
        val action: String? = intent.getAction()
        if (action.equals(CONNECTIVITY_CHANGE, ignoreCase = true)) {
            if (!isNetworkAvailable()) {
                showToast("Seems like your Internet is too slow or not available.")
            }
        }
    }

    companion object{
        fun isNetworkAvailable(): Boolean {
            val connectivityManager =
                AppObjectController.joshApplication.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                    // for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                val nwInfo = connectivityManager.activeNetworkInfo ?: return false
                return nwInfo.isConnected
            }
        }
    }
}