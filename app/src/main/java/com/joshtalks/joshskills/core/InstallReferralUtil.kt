package com.joshtalks.joshskills.core

import android.content.Context
import android.os.RemoteException
import android.text.TextUtils
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import io.branch.referral.PrefHelper
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import java.net.URLDecoder
import java.util.Date
import java.util.HashMap

object InstallReferralUtil {

    fun installReferrer(context: Context) {
        try {
            val obj = InstallReferrerModel.getPrefObject()
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
                                    val referrerMap = HashMap<String, String>()
                                    val referralParams = rawReferrerString.split("&").toTypedArray()
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
                                        installReferrerModel.utmMedium = referrerMap["utm_medium"]
                                        appAnalytics.addParam(
                                            AnalyticsEvent.UTM_MEDIUM.NAME,
                                            installReferrerModel.utmMedium
                                        )
                                    }
                                    if (referrerMap["utm_source"].isNullOrEmpty().not()) {
                                        installReferrerModel.utmSource = referrerMap["utm_source"]
                                        appAnalytics.addParam(
                                            AnalyticsEvent.SOURCE.NAME,
                                            installReferrerModel.utmSource
                                        )
                                    }
                                    if (response.installBeginTimestampSeconds > 0) {
                                        val instant =
                                            Instant.ofEpochSecond(response.installBeginTimestampSeconds)
                                        val time = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)

                                        installReferrerModel.installOn =
                                            (time.toEpochSecond())
                                    }
                                    if (installReferrerModel.installOn == 0L) {
                                        installReferrerModel.installOn = (Date().time / 1000)
                                    }
                                    InstallReferrerModel.update(installReferrerModel)
                                    appAnalytics.push()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    appAnalytics.push()
                                }
                            } catch (ex: RemoteException) {
                                PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.message)
                                FirebaseCrashlytics.getInstance().recordException(ex)
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