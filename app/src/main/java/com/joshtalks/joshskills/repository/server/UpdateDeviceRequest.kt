package com.joshtalks.joshskills.repository.server

import android.os.Build
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.Mentor

data class UpdateDeviceRequest(

    @SerializedName("device")
    internal var device: String = Build.DEVICE,
    @SerializedName("model")
    internal var model: String = Build.MODEL,
    @SerializedName("manufacture")
    internal var manufacture: String = Build.MANUFACTURER,
    @SerializedName("brand")
    internal var brand: String = Build.BRAND,
    @SerializedName("os_version_code")
    internal var osVersionCode: Int = Build.VERSION.SDK_INT,
    @SerializedName("os_version_name")
    internal var osVersionName: String = Build.VERSION.CODENAME,
    @SerializedName("app_version_code")
    internal var appVersionCode: Int = BuildConfig.VERSION_CODE,
    @SerializedName("app_version_name")
    internal var appVersionName: String = BuildConfig.VERSION_NAME,
    @SerializedName("device_id")
    internal var deviceId: String = Utils.getDeviceId(),
    @SerializedName("user_id")
    internal var user_id: String = Mentor.getInstance().getId(),
    @SerializedName("gaid")
    internal var gaid: String = PrefManager.getStringValue(USER_UNIQUE_ID)
)