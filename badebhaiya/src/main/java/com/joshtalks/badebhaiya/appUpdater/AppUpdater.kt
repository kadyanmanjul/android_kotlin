package com.joshtalks.badebhaiya.appUpdater

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
This class is responsible to manage force updates.
 */

@Singleton
class AppUpdater @Inject constructor() {

    companion object {
        const val APP_UPDATE = "app_update"
        const val APP_UPDATE_REQUEST_CODE = 8
    }

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateInfo: Task<AppUpdateInfo>
    private lateinit var appUpdateProperties: AppUpdate
    private var activity: AppCompatActivity? = null

    val isUpdateAvailable = MutableStateFlow(true)

    fun checkAndUpdate(context: AppCompatActivity) {

        try {
            activity = context
            appUpdateManager = AppUpdateManagerFactory.create(context)

            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    // Request the update.
                    checkIfStrictUpdate()
                } else {
                    isUpdateAvailable.value = false
                }
            }
        } catch (e:Exception){
            isUpdateAvailable.value = true
        }
    }

    private fun checkIfStrictUpdate() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        activity?.let {
            remoteConfig.fetchAndActivate().addOnCompleteListener(it) {
                if (it.isSuccessful) {
                    try {
                        val value = remoteConfig.getString(APP_UPDATE)
                        val appUpdateProperties = Gson().fromJson(value, AppUpdate::class.java)
                        appUpdateProperties?.let { appUpdate ->
                            if (appUpdate.isStrictUpdate()) {
                                startUpdating()
                            }
                        }
                    } catch (e: Exception) {
                        isUpdateAvailable.value = false
                    }
                } else {
                    isUpdateAvailable.value = false

                }
            }
        }

    }

    private fun startUpdating() {
        isUpdateAvailable.value = true
        activity?.let {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo.result,
                AppUpdateType.IMMEDIATE,
                it,
                APP_UPDATE_REQUEST_CODE
            )
        }
    }

    fun checkIfUpdating() {
        activity?.let { activity ->

            appUpdateInfo.addOnSuccessListener {
                when {
                    it.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            it,
                            IMMEDIATE,
                            activity,
                            APP_UPDATE_REQUEST_CODE
                        )
                    }
                    it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        // Request the update.
                        checkIfStrictUpdate()
                    }
                    else -> {
                        isUpdateAvailable.value = false
                    }
                }
            }
        }
    }

    fun onResult(requestCode: Int, resultCode: Int) {
        if (requestCode == APP_UPDATE_REQUEST_CODE && resultCode != RESULT_OK) {
                // If the update is cancelled or fails,
                // you can request to start the update again.
                // TODO: Show please update activity.
                if (activity != null){
                    ForceUpdateNoticeActivity.launch(activity!!)
                } else {
                    isUpdateAvailable.value = false
                }
        }
    }

}