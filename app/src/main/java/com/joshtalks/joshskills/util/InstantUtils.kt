package com.joshtalks.joshskills.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Reads the Instant App Cookie
 */
@RequiresApi(Build.VERSION_CODES.O)
fun Context.readCookie() = packageManager.instantAppCookie.toString(Charsets.UTF_8)

/**
 * Clear the Instant App Cookie
 */
@RequiresApi(Build.VERSION_CODES.O)
fun Context.clearCookie(): String {
    packageManager.clearInstantAppCookie()
    return "Cookie cleared"
}