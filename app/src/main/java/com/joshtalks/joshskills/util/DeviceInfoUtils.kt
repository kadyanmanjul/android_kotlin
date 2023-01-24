package com.joshtalks.joshskills.util

import android.os.Build

object  DeviceInfoUtils {

    var manufacturer: String = Build.MANUFACTURER
    var Brand_value: String = Build.BRAND
    var Model_value: String = Build.MODEL
    var Hardware_value: String = Build.HARDWARE
    var Build_ID: String = Build.ID
    var Fingerprint: String = Build.FINGERPRINT.toString()
    var version = Build.VERSION.SDK_INT

    fun  getDetails(): String {
        return """
            Vendor : $manufacturer
            Version : $version
            Brand : $Brand_value
            MODEL : $Model_value
            HARDWARE : $Hardware_value
            ID : $Build_ID
            FINGERPRINT : $Fingerprint
            """.trimIndent()
    }
}