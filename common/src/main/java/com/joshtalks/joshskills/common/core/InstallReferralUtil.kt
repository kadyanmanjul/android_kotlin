package com.joshtalks.joshskills.common.core

import android.content.Context
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.repository.local.model.InstallReferrerModel
import io.branch.referral.PrefHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URLDecoder
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.collections.toTypedArray

object InstallReferralUtil {

    fun installReferrer(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = InstallReferrerModel.getPrefObject()
                Timber.d("refer123 : $obj")
                val appAnalytics = AppAnalytics.create(AnalyticsEvent.APP_INSTALL.NAME)
                if (obj == null) {
                    val referrerClient = InstallReferrerClient.newBuilder(context).build()
                    referrerClient.startConnection(object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            when (responseCode) {
                                InstallReferrerClient.InstallReferrerResponse.OK -> try {
                                    appAnalytics
                                        .addBasicParam()
                                        .addUserDetails()
                                    try {
                                        val response = referrerClient.installReferrer
                                        val rawReferrerString =
                                            URLDecoder.decode(response.installReferrer, "UTF-8")
                                        Log.d(
                                            TAG,
                                            "onInstallReferrerSetupFinished: $rawReferrerString"
                                        )
                                        val  referrerMap = HashMap<String, String>()
                                        val referralParams =
                                            rawReferrerString.split("&").toTypedArray()
                                        for (referrerParam in referralParams) {
                                            if (!TextUtils.isEmpty(referrerParam)) {
                                                var splitter = "="
                                                if (!referrerParam.contains("=") && referrerParam.contains(
                                                        "-"
                                                    )
                                                ) {
                                                    splitter = "-"
                                                }
                                                val keyValue =
                                                    referrerParam.split(splitter).toTypedArray()
                                                if (keyValue.size > 1) { // To make sure that there is one key value pair in referrer
                                                    referrerMap[URLDecoder.decode(
                                                        keyValue[0],
                                                        "UTF-8"
                                                    )] =
                                                        URLDecoder.decode(keyValue[1], "UTF-8")
                                                }
                                            }
                                        }
                                        val installReferrerModel = InstallReferrerModel()
                                        installReferrerModel.otherInfo = referrerMap
                                        installReferrerModel.otherInfo?.apply {
                                            this["install_begin_on"] =
                                                response.installBeginTimestampSeconds.toString()
                                            this["referral_click_on"] =
                                                response.referrerClickTimestampSeconds.toString()
                                        }

                                        if (referrerMap["utm_medium"].isNullOrEmpty().not()) {
                                            installReferrerModel.utmMedium =
                                                referrerMap["utm_medium"]
                                            appAnalytics.addParam(
                                                AnalyticsEvent.UTM_MEDIUM.NAME,
                                                installReferrerModel.utmMedium
                                            )
                                        }
                                        if (referrerMap["utm_source"].isNullOrEmpty().not()) {
                                            installReferrerModel.utmSource =
                                                referrerMap["utm_source"]
                                            appAnalytics.addParam(
                                                AnalyticsEvent.SOURCE.NAME,
                                                installReferrerModel.utmSource
                                            )
                                        }
                                        if (referrerMap["utm_campaign"].isNullOrEmpty().not()) {
                                            installReferrerModel.utmTerm =
                                                referrerMap["utm_campaign"]
                                            appAnalytics.addParam(
                                                AnalyticsEvent.SOURCE.NAME,
                                                installReferrerModel.utmTerm
                                            )
                                        }
//                                    if (response.installBeginTimestampSeconds > 0) {
//                                        val instant =
//                                            Instant.ofEpochSecond(response.installBeginTimestampSeconds)
//                                        val time = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
//
//                                        installReferrerModel.installOn =
//                                            (time.toEpochSecond())
//                                    }
                                        if (installReferrerModel.installOn == 0L) {
                                            installReferrerModel.installOn = (Date().time / 1000)
                                        }
                                        InstallReferrerModel.update(installReferrerModel)
                                        Log.i(TAG, "onInstallReferrerSetupFinished: $installReferrerModel")
                                        appAnalytics.push()
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                        appAnalytics.push()
                                    }
                                } catch (ex: RemoteException) {
                                    PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.message)
                                    try {
                                        FirebaseCrashlytics.getInstance().recordException(ex)
                                    }catch (ex:Exception){

                                    }
                                    ex.printStackTrace()
                                }
                            }
                        }

                        override fun onInstallReferrerServiceDisconnected() {
                            PrefHelper.Debug("onInstallReferrerServiceDisconnected()")
                        }
                    })
                }
                //Timber.tag("JoshReferral").e(InstallReferrerModel.getPrefObject().toString())

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
