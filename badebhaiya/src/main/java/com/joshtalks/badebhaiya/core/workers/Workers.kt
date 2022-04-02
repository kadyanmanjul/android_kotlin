package com.joshtalks.badebhaiya.core.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.joshtalks.badebhaiya.core.API_TOKEN
import com.joshtalks.badebhaiya.core.FCM_TOKEN
import com.joshtalks.badebhaiya.core.INSTALL_REFERRER_SYNC
import com.joshtalks.badebhaiya.core.InstallReferralUtil
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.models.DeviceDetailsResponse
import com.joshtalks.badebhaiya.core.models.InstallReferrerModel
import com.joshtalks.badebhaiya.core.models.UpdateDeviceRequest
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.ApiRespStatus
import com.joshtalks.badebhaiya.utils.AppSignatureHelper.TAG
import com.joshtalks.badebhaiya.utils.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class AppRunRequiredTaskWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
            PrefManager.put(API_TOKEN, User.getInstance().token)
        }
        InstallReferralUtil.installReferrer(context)
        return Result.success()
    }
}

class JoshTalksInstallWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (PrefManager.hasKey(INSTALL_REFERRER_SYNC)) {
            return Result.success()
        }
        val obj = InstallReferrerModel.getPrefObject()
        obj?.apply {
            this.user = User.getInstance().userId
        }
        if (obj != null) {
            try {
                CommonRepository().getInstallReferrerAsync(obj)
                PrefManager.put(INSTALL_REFERRER_SYNC, true)
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }
        return Result.success()
    }
}

class RefreshFCMTokenWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            FirebaseInstallations.getInstance().delete().addOnCompleteListener {
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                    regenerateFCM()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return Result.failure()
        }
        return Result.success()
    }

    private fun regenerateFCM() {
        PrefManager.removeKey(FCM_TOKEN)
        FirebaseInstallations.getInstance().getToken(true).addOnCompleteListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                OnCompleteListener { task ->
                    Timber.d(TAG+" : Refreshed")
                    if (!task.isSuccessful) {
                        task.exception?.run {
                            LogException.catchException(this)
                        }
                        task.exception?.printStackTrace()
                        return@OnCompleteListener
                    }
                    task.result.let {
                        val fcmResponse = FCMData.getInstance()
                        fcmResponse?.apiStatus = ApiRespStatus.POST
                        fcmResponse?.update()
                        Timber.d(TAG+" : Updated")
                        CoroutineScope(
                            SupervisorJob() +
                                    Dispatchers.IO +
                                    CoroutineExceptionHandler { _, _ -> /* Do Nothing */ }
                        ).launch {
                            try {
                                val userId = User.getInstance().userId
                                if (userId.isNotBlank()) {
                                    try {
                                        if (PrefManager.hasKey(FCM_TOKEN)) {
                                            val data = mutableMapOf(
                                                "registration_id" to it
                                            )
                                            val resp =
                                                CommonRepository().patchFCMToken(userId, data)
                                            if (resp.isSuccessful) {
                                                resp.body()?.update()
                                                PrefManager.put(FCM_TOKEN, it)
                                                Timber.tag(FCMData::class.java.name)
                                                    .e("patch data : ${resp.body()}")
                                            }
                                        } else {
                                            val data = mutableMapOf(
                                                "name" to Utils.getDeviceName(),
                                                "registration_id" to it,
                                                "device_id" to Utils.getDeviceId(),
                                                "active" to "true",
                                                "user_id" to userId,
                                                "type" to "android"
                                            )
                                            val resp = CommonRepository().postFCMToken(data)
                                            if (resp.isSuccessful) {
                                                resp.body()?.update()
                                                PrefManager.put(FCM_TOKEN, it)
                                                Timber.tag(FCMData::class.java.name)
                                                    .e("post data : ${resp.body()}")
                                            }
                                        }
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            )
        }
    }
}

class UpdateDeviceDetailsWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "doWork() called")
            val device = DeviceDetailsResponse.getInstance()
            val status = device?.apiStatus ?: ApiRespStatus.EMPTY
            val deviceId = device?.id ?: 0
            if (ApiRespStatus.PATCH == status) {
                //return Result.success()
                if (deviceId > 0) {
                    val details = CommonRepository().patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else if (ApiRespStatus.POST == status) {
                if (deviceId > 0) {
                    val details = CommonRepository().patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    // TODO no need to send UpdateDeviceRequest object in patch request 
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else {
                val details =
                    CommonRepository().postDeviceDetails(
                        UpdateDeviceRequest()
                    )
                details.apiStatus = ApiRespStatus.POST
                details.update()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}
