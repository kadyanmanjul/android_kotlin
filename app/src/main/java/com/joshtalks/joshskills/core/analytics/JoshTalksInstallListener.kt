package com.joshtalks.joshskills.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.*

class JoshTalksInstallListener : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.hasExtra("referrer")?.let {
            val rawReferrerString = intent.getStringExtra("referrer")
            processReferrer(rawReferrerString)
        }
    }

    private fun processReferrer(referrerString: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawReferrerString = URLDecoder.decode(referrerString, "UTF-8")
                val referrerMap = HashMap<String, String>()
                val referralParams = rawReferrerString.split("&").toTypedArray()
                for (referrerParam in referralParams) {
                    if (!TextUtils.isEmpty(referrerParam)) {
                        var splitter = "="
                        if (!referrerParam.contains("=") && referrerParam.contains("-")) {
                            splitter = "-"
                        }
                        val keyValue = referrerParam.split(splitter).toTypedArray()
                        if (keyValue.size > 1) { // To make sure that there is one key value pair in referrer
                            referrerMap[URLDecoder.decode(keyValue[0], "UTF-8")] =
                                URLDecoder.decode(keyValue[1], "UTF-8")
                        }
                    }
                }
                val obj = InstallReferrerModel()
                obj.referrer = referrerMap
                InstallReferrerModel.update(obj)

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }


}
