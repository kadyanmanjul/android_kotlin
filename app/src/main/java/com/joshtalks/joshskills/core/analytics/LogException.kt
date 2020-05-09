package com.joshtalks.joshskills.core.analytics

import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.BuildConfig
import com.newrelic.agent.android.NewRelic
import io.sentry.core.Sentry

object LogException {
    fun catchException(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace()
            return
        }
        Sentry.captureException(throwable)
        NewRelic.recordHandledException(throwable as Exception)
        Crashlytics.logException(throwable)

    }
}