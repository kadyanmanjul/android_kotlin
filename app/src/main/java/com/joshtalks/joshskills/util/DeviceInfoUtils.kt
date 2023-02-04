package com.joshtalks.joshskills.util

import android.os.Build
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.freetrail.BuildConfig.VERSION_CODE
import com.joshtalks.joshskills.freetrail.BuildConfig.VERSION_NAME

object  DeviceInfoUtils {

    var manufacturer: String = Build.MANUFACTURER
    var Brand_value: String = Build.BRAND
    var Model_value: String = Build.MODEL
    var Hardware_value: String = Build.HARDWARE
    var Build_ID: String = Build.ID
    var Fingerprint: String = Build.FINGERPRINT.toString()
    var version = Build.VERSION.SDK_INT
    var apkVersionCode = BuildConfig.VERSION_CODE
    var apkVersionName = BuildConfig.VERSION_NAME

    fun getMobileDetails():HashMap<String,Any>{
        val map = HashMap<String,Any>()
        map["Vendor"] = manufacturer
        map["Version"] = version
        map["Brand"] = Brand_value
        map["MODEL"] = Model_value
        map["HARDWARE"] = Hardware_value
        map["ID"] = Build_ID
        map["FINGERPRINT"] = Fingerprint
        map["APK_VERSION_CODE"] = apkVersionCode
        map["APK_VERSION_NAME"] = apkVersionName
        return map
    }
}