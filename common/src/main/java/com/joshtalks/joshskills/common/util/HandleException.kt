package com.joshtalks.joshskills.common.util

import android.app.Application
import com.google.firebase.FirebaseNetworkException
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.analytics.LogException
import com.joshtalks.joshskills.common.core.showToast
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.showAppropriateMsg(application: Application = AppObjectController.joshApplication) {
    when (this) {
        is HttpException -> {
            showToast(application.getString(R.string.something_went_wrong))
        }
        is SocketTimeoutException, is UnknownHostException, is FirebaseNetworkException -> {
            showToast(application.getString(R.string.internet_not_available_msz))
        }
        else -> {
            showToast(application.getString(R.string.something_went_wrong))
        }
    }
}

fun Throwable.showAppropriateMsg(application: Application = AppObjectController.joshApplication) {
    LogException.catchException(this)
    when (this) {
        is HttpException -> {
            showToast(application.getString(R.string.something_went_wrong))
        }
        is SocketTimeoutException, is UnknownHostException, is FirebaseNetworkException -> {
            showToast(application.getString(R.string.internet_not_available_msz))
        }
        else -> {
            showToast(application.getString(R.string.something_went_wrong))
        }
    }
}
