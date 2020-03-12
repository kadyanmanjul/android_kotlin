package com.joshtalks.joshskills.core

import android.content.Context
import android.os.RemoteException
import android.text.TextUtils
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import io.branch.referral.PrefHelper
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import java.net.URLDecoder
import java.util.*

object InstallReferralUtil {

    fun installReferrer(context: Context) {
        try {
            val obj = InstallReferrerModel.getPrefObject()
            if (obj == null) {
                val referrerClient = InstallReferrerClient.newBuilder(context).build()
                referrerClient.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> try {
                                AppAnalytics.create(AnalyticsEvent.APP_INSTALL.NAME).push()

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
                                    }
                                    if (referrerMap["utm_source"].isNullOrEmpty().not()) {
                                        installReferrerModel.utmSource = referrerMap["utm_source"]
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

                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }

                            } catch (ex: RemoteException) {
                                PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.message)
                                Crashlytics.logException(ex)
                                ex.printStackTrace()
                            }
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        PrefHelper.Debug("onInstallReferrerServiceDisconnected()")
                    }
                })
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}