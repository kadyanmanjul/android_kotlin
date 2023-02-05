package com.joshtalks.joshskills.premium.core

import android.util.Log
import com.joshtalks.joshskills.base.core.RetrofitNetwork
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.SPEED_TEST_FILE_SIZE
import com.joshtalks.joshskills.premium.core.SPEED_TEST_FILE_URL
import com.joshtalks.joshskills.premium.core.THRESHOLD_SPEED_IN_KBPS

object ConnectionDetails {
    private val TAG = "ConnectionDetails"
    // Download file
    val apiService by lazy {
        RetrofitNetwork.getNetworkSpeedApi()
    }

    private suspend fun downloadTime() : Long {
        try {
            val startTime = System.currentTimeMillis()
            val response = apiService.downloadSpeedTestFile(
                PrefManager.getStringValue(
                    SPEED_TEST_FILE_URL
                ))
            return if(response.isSuccessful)
                System.currentTimeMillis() - startTime
            else
                Long.MAX_VALUE
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return Long.MAX_VALUE
    }

    suspend fun getInternetSpeed() : Speed {
        val timeInSec = downloadTime() / 1000F
        val fileSizeInBits = PrefManager.getIntValue(SPEED_TEST_FILE_SIZE) * 1024 * 8F
        val avgSpeedInKbps = (fileSizeInBits / timeInSec) / 1024
        Log.d(TAG, "getInternetSpeed: $avgSpeedInKbps")
        return if(avgSpeedInKbps < PrefManager.getIntValue(THRESHOLD_SPEED_IN_KBPS)) Speed.LOW else Speed.HIGH
    }
}

enum class Speed {
    LOW,
    NORMAL,
    HIGH
}