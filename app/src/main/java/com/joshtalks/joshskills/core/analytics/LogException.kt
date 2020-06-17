package com.joshtalks.joshskills.core.analytics

import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.BuildConfig
import com.newrelic.agent.android.NewRelic

object LogException {

    fun catchException(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace()
            return
        }
        NewRelic.recordHandledException(throwable as Exception)
        Crashlytics.logException(throwable)
    }

    fun catchError(tag: ErrorTag, error: String) {
        if (BuildConfig.DEBUG) {
            return
        }
        NewRelic.recordCustomEvent(tag.name, mutableMapOf())
        Crashlytics.log(3, tag.NAME, error)

    }
}

enum class ErrorTag(val NAME: String) {
    TRUE_CALLER("TrueCaller"),
    OTP_REQUEST("OTP Request"),
    AUDIO_RECORDER("Audio Recorder")
}
