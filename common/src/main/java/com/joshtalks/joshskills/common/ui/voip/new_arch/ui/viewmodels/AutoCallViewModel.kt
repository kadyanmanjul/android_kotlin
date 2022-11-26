package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels

import android.app.Application
import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.voip.base.eval
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_SCREEN_DATA
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_SCREEN_DATA_DEFAULT
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_SCREEN_TIPS
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_TIPS_DEFAULT
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models.AutoConnectData
import com.joshtalks.joshskills.voip.constant.CALL_NOW
import com.joshtalks.joshskills.voip.constant.STOP_WAITING
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.EventName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Type
import kotlin.random.Random

private const val TAG = "AutoCallViewModel"

class AutoCallViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {
    private var singleLiveEvent = com.joshtalks.joshskills.common.base.EventLiveData
    var isCalled = false
    val mutex = Mutex()
    private val gson = Gson()
    val header = ObservableField<String>()
    val subHeader = ObservableField<String>()
    val tips = ObservableField<String>()

    fun callNow(view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isCalled.not())
                mutex.withLock {
                    if (isCalled.not()) {
                        val msg = Message.obtain().apply {
                            what = CALL_NOW
                        }
                        withContext(Dispatchers.Main) {
                            singleLiveEvent.value = msg
                        }
                        isCalled = true
                    }
                }
        }
    }

    fun stopWaiting(view: View) {
        val msg = Message.obtain().apply {
            what = STOP_WAITING
        }
        singleLiveEvent.value = msg
    }

    suspend fun setUIState(): AutoConnectData? {
        CallAnalytics.addAnalytics(
            event = EventName.AUTO_CONNECT_SCREEN_SHOWN,
            agoraCallId = "",
            agoraMentorId = com.joshtalks.joshskills.voip.data.local.PrefManager.getLocalUserAgoraId().toString()
        )
        setTipsUI()
        val json = fetchScreenJson(AUTO_CONNECT_SCREEN_DATA, AUTO_CONNECT_SCREEN_DATA_DEFAULT)
        Log.d(TAG, "setUIState: $json")
        val listType: Type = object : TypeToken<List<AutoConnectData?>?>() {}.type
        val autoConnectList: List<AutoConnectData> = gson.fromJson(json, listType)
        for (autoConnect in autoConnectList) {
            if (autoConnect.condition?.eval(VoipPref.getLastCallDurationInSec()) == true) {
                header.set(autoConnect.header)
                subHeader.set(autoConnect.subHeader)
                return autoConnect
            }
        }
        return null
    }

    private fun fetchScreenJson(firebaseKey: String, defaultValueKey: String): String {
            return AppObjectController.getFirebaseRemoteConfig()
                .getString(
                    "${firebaseKey}${
                        PrefManager.getStringValue(
                            CURRENT_COURSE_ID
                        )
                    }"
                ).ifBlank {
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(defaultValueKey)
                }
    }

    private fun setTipsUI() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = fetchScreenJson(AUTO_CONNECT_SCREEN_TIPS, AUTO_CONNECT_TIPS_DEFAULT)
                Log.d(TAG, "setTipsUI: $json")
                val listType: Type = object : TypeToken<List<String?>?>() {}.type
                val tipsList: List<String> = gson.fromJson(json, listType)
                val randomIndex = Random.Default.nextInt(0, tipsList.size)
                tips.set(tipsList[randomIndex])
            } catch (e : Exception) {
                e.printStackTrace()
                Log.e(TAG, "setTipsUI: Error ${e.message}")
            }
        }
    }
}