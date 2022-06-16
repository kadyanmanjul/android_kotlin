package com.joshtalks.joshskills.base.core

import android.util.Log

private const val TAG = "ConnectionDetails"
private const val THRESHOLD_SPEED_IN_KBPS = 16

object ConnectionDetails {
    // Download file
    val apiService by lazy {
        RetrofitNetwork.getNetworkSpeedApi()
    }

    private suspend fun downloadTime() : Long {
        try {
            val startTime = System.currentTimeMillis()
            val response = apiService.downloadSpeedTestFile()
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
        val fileSizeInBits = 157 * 1024 * 8F
        val avgSpeedInKbps = (fileSizeInBits / timeInSec) / (1024 * 8F)
        Log.d(TAG, "getInternetSpeed: $avgSpeedInKbps")
        return if(avgSpeedInKbps < THRESHOLD_SPEED_IN_KBPS) Speed.LOW else Speed.HIGH
    }
}

enum class Speed {
    LOW,
    NORMAL,
    HIGH
}