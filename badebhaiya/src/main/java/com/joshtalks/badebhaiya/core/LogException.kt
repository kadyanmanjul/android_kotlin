package com.joshtalks.badebhaiya.core

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.badebhaiya.BuildConfig
import timber.log.Timber

object LogException {

    fun catchException(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Timber.e(throwable)
            throwable.printStackTrace()
        }
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun catchError(tag: ErrorTag, error: String) {
        FirebaseCrashlytics.getInstance().log("(" + tag.NAME + ") - " + error)

    }
}

enum class ErrorTag(val NAME: String) {
    TRUE_CALLER("TrueCaller"),
    OTP_REQUEST("OTP Request"),
    AUDIO_RECORDER("Audio Recorder")
}
