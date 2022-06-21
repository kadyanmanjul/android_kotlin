package com.joshtalks.badebhaiya.appUpdater

import android.app.Activity.RESULT_OK
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private lateinit var appUpdateInfo: AppUpdateInfo
    private var activity: AppCompatActivity? = null
    private lateinit var jobs: MutableList<Job>

//    private var updateRequestCode = 111

    val isUpdateAvailable = MutableStateFlow(true)
    val onDownloadClick = MutableSharedFlow<Boolean>()

    fun checkAndUpdate(context: AppCompatActivity) {

        try {
//            updateRequestCode = 111
            jobs = mutableListOf()
            collectDownloadClick()
            activity = context
            appUpdateManager = AppUpdateManagerFactory.create(context)

              val updateInfo = appUpdateManager.appUpdateInfo

            updateInfo.addOnSuccessListener {
                this.appUpdateInfo = it
                if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
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
            APP_UPDATE_REQUEST_CODE
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                it,
                APP_UPDATE_REQUEST_CODE
            )
        }
    }

    fun checkIfUpdating() {
        activity?.let { activity ->

            appUpdateManager.appUpdateInfo.addOnSuccessListener {
                this.appUpdateInfo = it
                when {
                    it.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // If an in-app update is already running, resume the update.
//                        updateRequestCode++
                        appUpdateManager.startUpdateFlowForResult(
                            it,
                            IMMEDIATE,
                            activity,
                            APP_UPDATE_REQUEST_CODE
                        )
                    }
                    it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        // Request the update.
//                        updateRequestCode++
//                        appUpdateManager.startUpdateFlowForResult(
//                            it,
//                            IMMEDIATE,
//                            activity,
//                            APP_UPDATE_REQUEST_CODE
//                        )
//                        checkIfStrictUpdate()
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
                // Show please update activity.
//            checkAndUpdate(activity!!)
//            startUpdating()
    Timber.tag("APPUPDATEMANAGER").d("APP UPDATE IS CANCELLED AND RESULT => $resultCode")
               launchForceUpdateNotice()
        } else if (requestCode == APP_UPDATE_REQUEST_CODE && resultCode == RESULT_OK){
            isUpdateAvailable.value = false
        }
    }

    private fun launchForceUpdateNotice() {
        if (activity != null){
            ForceUpdateNoticeActivity.launch(activity!!)
        } else {
            isUpdateAvailable.value = false
        }
    }

    private fun collectDownloadClick(){
        jobs += CoroutineScope(Dispatchers.IO).launch {
           onDownloadClick.collectLatest { downloadClicked ->
               if (downloadClicked){
//                   launchForceUpdateNotice()
                   checkIfStrictUpdate()
               }
           }
        }
    }

    fun onDownloadClick(){
        jobs += CoroutineScope(Dispatchers.IO).launch {
            onDownloadClick.emit(true)
        }
    }

    fun flushResources(){
        jobs.forEach {
            it.cancel()
        }
    }

}