package com.joshtalks.joshskills.common.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.common.BuildConfig
import timber.log.Timber

object LogException {

    fun catchException(throwable: Throwable) {
        try {
            if (BuildConfig.DEBUG) {
                Timber.e(throwable)
                throwable.printStackTrace()
            }
            //  NewRelic.recordHandledException(throwable as Exception)
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }catch (ex:Exception){

        }
    }

    fun catchError(tag: ErrorTag, error: String) {
        try {
            if (BuildConfig.DEBUG) {
                //return
            }
            // NewRelic.recordCustomEvent(tag.name, mutableMapOf())
            FirebaseCrashlytics.getInstance().log("(" + tag.NAME + ") - " + error)
        }catch (ex:Exception){

        }
    }
}

enum class ErrorTag(val NAME: String) {
    TRUE_CALLER("TrueCaller"),
    OTP_REQUEST("OTP Request"),
    AUDIO_RECORDER("Audio Recorder"),
    FFMPEG_ERROR_LOAD("ffmpeg lib error")
}
