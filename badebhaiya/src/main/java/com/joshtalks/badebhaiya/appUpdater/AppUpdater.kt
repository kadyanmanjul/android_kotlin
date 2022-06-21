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
import dagger.hilt.android.qualifiers.ApplicationContext
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
class JoshAppUpdater @Inject constructor() {

    companion object {
        const val APP_UPDATE = "app_update"
        const val APP_UPDATE_REQUEST_CODE = 8
    }

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateInfo: Task<AppUpdateInfo>
    private var activity: AppCompatActivity? = null

    private var updateRequestCode = 111

    val isUpdateAvailable = MutableStateFlow(true)

    fun checkAndUpdate(context: AppCompatActivity) {

        try {
            updateRequestCode = 111
            activity = context
            appUpdateManager = AppUpdateManagerFactory.create(context)

             appUpdateInfo = appUpdateManager.appUpdateInfo

            appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
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
//        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
//        val configSettings = remoteConfigSettings {
//            minimumFetchIntervalInSeconds = 0
//        }
//        remoteConfig.setConfigSettingsAsync(configSettings)
        val remoteConfig = Firebase.remoteConfig
        activity?.let {
            remoteConfig.fetchAndActivate().addOnCompleteListener(it) {
                if (it.isSuccessful) {
                    try {
                        val value = remoteConfig.getString(APP_UPDATE)
                        val appUpdateProperties = Gson().fromJson(value, AppUpdate::class.java)
                        appUpdateProperties?.let { appUpdate ->
                            if (appUpdate.isStrictUpdate()) {
                                startUpdating()
                            } else {
                                isUpdateAvailable.value = false
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
            updateRequestCode++
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo.result,
                AppUpdateType.IMMEDIATE,
                it,
                updateRequestCode
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
                        updateRequestCode++
                        appUpdateManager.startUpdateFlowForResult(
                            it,
                            IMMEDIATE,
                            activity,
                            updateRequestCode
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
        if (requestCode == updateRequestCode && resultCode != RESULT_OK) {
                // Show please update activity.
            checkAndUpdate(activity!!)
//            startUpdating()
//                if (activity != null){
//                    ForceUpdateNoticeActivity.launch(activity!!)
//                } else {
//                    isUpdateAvailable.value = false
//                }
        }
    }

}