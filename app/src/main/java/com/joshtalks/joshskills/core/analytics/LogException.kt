package com.joshtalks.joshskills.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.BuildConfig
import io.sentry.Sentry
import timber.log.Timber

object LogException {

    fun catchException(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Timber.e(throwable)
            throwable.printStackTrace()
            //return
        }
      //  NewRelic.recordHandledException(throwable as Exception)
        //Sentry.captureException(throwable)
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun catchError(tag: ErrorTag, error: String) {
        if (BuildConfig.DEBUG) {
           //return
        }
        // NewRelic.recordCustomEvent(tag.name, mutableMapOf())
        //Sentry.captureMessage(tag.name + error, SentryLevel.ERROR)
        FirebaseCrashlytics.getInstance().log("(" + tag.NAME + ") - " + error)

    }
}

enum class ErrorTag(val NAME: String) {
    TRUE_CALLER("TrueCaller"),
    OTP_REQUEST("OTP Request"),
    AUDIO_RECORDER("Audio Recorder")
}
