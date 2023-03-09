package com.joshtalks.joshskills.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.instantapps.InstantApps

/**
 * Reads the Instant App Cookie
 */
@RequiresApi(Build.VERSION_CODES.O)
fun Context.readCookie() : String {
    val packageManager = InstantApps.getPackageManagerCompat(applicationContext)
    return packageManager.instantAppCookie?.toString(Charsets.UTF_8) ?: "Instant App Cookie is NULL"
}

/**
 * Clear the Instant App Cookie
 */
@RequiresApi(Build.VERSION_CODES.O)
fun Context.clearCookie(): String {
    val packageManager = InstantApps.getPackageManagerCompat(applicationContext)
    packageManager.instantAppCookie = null
    return "Cookie cleared"
}