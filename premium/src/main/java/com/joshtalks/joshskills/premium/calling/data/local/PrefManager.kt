package com.joshtalks.joshskills.premium.calling.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_DISCONNECT_SCREEN
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.calling.Utils
import com.joshtalks.joshskills.premium.calling.constant.Category
import com.joshtalks.joshskills.premium.calling.constant.PREF_KEY_PSTN_STATE
import com.joshtalks.joshskills.premium.calling.constant.PSTN_STATE_IDLE
import com.joshtalks.joshskills.premium.calling.constant.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import timber.log.Timber
import kotlin.math.log

const val LATEST_PUBNUB_MESSAGE_TIME = "josh_pref_key_latest_pubnub_message_time"
const val VOIP_STATE = "josh_pref_key_voip_state"
const val INCOMING_CALL = "josh_pref_key_incoming_call"
const val LOCAL_USER_AGORA_ID = "josh_pref_key_local_user_agora_id"
const val AGORA_CALL_ID = "josh_pref_key_agora_call_id"
const val CURRENT_CALL_CATEGORY = "josh_pref_key_call_category"
const val LAST_RECORDING = "josh_pref_key_agora_call_recording"
const val EXPERT_CALL_DURATION = "EXPERT_CALL_DURATION"
const val PROXIMITY_ON = "is_josh_proximity_on"
const val IS_BEEP_TIMER_ENABLED = "IS_BEEP_TIMER_ENABLED"
const val IS_EXPERT_PREMIUM_USER = "is_expert_premium_user"
const val IS_VOIP_SERVICE_USED = "is_voip_service_used"

private const val TAG = "PrefManager"

class PrefManager {
    companion object {
        @Volatile lateinit var preferenceManager: SharedPreferences
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
        val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

        @Synchronized
        fun initServicePref(context: Context) {
            preferenceManager = context.getSharedPreferences(
                context.getString(R.string.voip_service_shared_pref_file_name),
                Context.MODE_PRIVATE
            )
        }

        fun isPrefManagerInitialize():Boolean {
            return ::preferenceManager.isInitialized
        }

        fun getLatestPubnubMessageTime(): Long {
            return preferenceManager.getLong(LATEST_PUBNUB_MESSAGE_TIME, 0L)
        }

        fun setLatestPubnubMessageTime(timetoken: Long) {
            val editor = preferenceManager.edit()
            editor.putLong(LATEST_PUBNUB_MESSAGE_TIME, timetoken)
            editor.apply()
        }

        fun getVoipState(): State {
            if(::preferenceManager.isInitialized.not())
                Utils.context?.let { initServicePref(it) }
            val ordinal = preferenceManager.getInt(VOIP_STATE, State.IDLE.ordinal)
            Log.d(TAG, "getVoipState: $ordinal")
            Log.d(TAG, "getVoipState: ${State.values()}")
            return State.values()[ordinal]
        }

        fun setVoipState(state: State) {
            Log.d(TAG, "Setting Voip State : $state")
            Log.d(TAG, "Setting Voip State : ${state.ordinal}")
            val editor = preferenceManager.edit()
            editor.putInt(VOIP_STATE, state.ordinal)
            editor.commit()
        }

        fun isProximitySensorOn() = preferenceManager.getBoolean(PROXIMITY_ON, true)

        fun updateProximitySettings(isProximityOn : Boolean) {
            Log.d("ProximityHelper", "updateProximitySettings: isProximitySensorOn = $isProximityOn")
            val editor = preferenceManager.edit()
            editor.putBoolean(PROXIMITY_ON, isProximityOn)
            editor.commit()
        }

        fun getCallCategory(): Category {
            val ordinal =
                preferenceManager.getInt(CURRENT_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            Log.d(TAG, "getCallCategory : $ordinal")
            Log.d(TAG, "getCallCategory : ${State.values()}")
            return Category.values()[ordinal]
        }

        fun setCallCategory(category: Category) {
            Log.d(TAG, "Setting Call Category : $category")
            Log.d(TAG, "Setting Call Category #: ${category.ordinal}")
            val editor = preferenceManager.edit()
            editor.putInt(CURRENT_CALL_CATEGORY, category.ordinal)
            editor.commit()
        }

        fun getIncomingCallId(): Int {
            return preferenceManager.getInt(INCOMING_CALL, -1)
        }

        fun setIncomingCallId(callId: Int) {
            val editor = preferenceManager.edit()
            editor.putInt(INCOMING_CALL, callId)
            editor.commit()
        }

        fun getLocalUserAgoraId(): Int {
            return preferenceManager.getInt(LOCAL_USER_AGORA_ID, -1)
        }

        fun getAgraCallId(): Int {
            return preferenceManager.getInt(AGORA_CALL_ID, -1)
        }

        fun setLocalUserAgoraIdAndCallId(localUserAgoraId: Int, callId: Int) {
            val editor = preferenceManager.edit()
            editor.putInt(LOCAL_USER_AGORA_ID, localUserAgoraId)
            editor.putInt(AGORA_CALL_ID, callId)
            editor.commit()
        }

        fun setExpertCallDuration(duration: String) {
            val editor = preferenceManager.edit()
            editor.putString(EXPERT_CALL_DURATION, duration)
            Log.d("calltime", "setExpertCallDuration time => $duration ")
            editor.commit()
        }

        fun getExpertCallDuration(): String? {
            return preferenceManager.getString(EXPERT_CALL_DURATION, "")
        }

        fun savePstnState(state: String) {
            Log.d(TAG, "Setting pstn State : $state")
            val editor = preferenceManager.edit()
            editor.putString(PREF_KEY_PSTN_STATE, state)
            editor.commit()
        }

        fun getPstnState(): String {
            Log.d(TAG, "Getting pstn State")
            return preferenceManager.getString(PREF_KEY_PSTN_STATE, PSTN_STATE_IDLE).toString()
        }

        fun saveLastRecordingPath(path: String) {
            Log.d(TAG, "saveLastRecordingPath State : $path")
            val editor = preferenceManager.edit()
            editor.putString(LAST_RECORDING, path)
            editor.commit()
        }

        fun getLastRecordingPath(): String {
            Log.d(TAG, "Getting getLastRecordingPath")
            return preferenceManager.getString(LAST_RECORDING, "").toString()
        }

        fun saveDisconnectScreen(screenName : String)  {
            Log.d(TAG, "saveDisconnectScreen: ")
            val editor = preferenceManager.edit()
            editor.putString(PREF_KEY_LAST_DISCONNECT_SCREEN, screenName)
            editor.commit()
        }

        fun getLastDisconnectScreenName() : String {
            return preferenceManager.getString(PREF_KEY_LAST_DISCONNECT_SCREEN, "NA") ?: "NA"
        }

        fun putBitmap(bitmap: Bitmap){
            preferenceManager.edit().remove("bitmap")?.apply()
            val editor = preferenceManager.edit()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val compressImage: ByteArray = baos.toByteArray()
            val sEncodedImage: String = Base64.encodeToString(compressImage, Base64.DEFAULT)
            editor.putString("bitmap", sEncodedImage)
            editor.commit()
        }

        fun getBitmap(): ByteArray?{
            if (preferenceManager.contains("bitmap")) {
                val encodedImage: String? = preferenceManager.getString("bitmap",null)
                val b: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
                return b
            }
            return null
        }

        fun setBeepTimerStatus(status: Int) {
            val isEnabled = status == 1
            val editor = preferenceManager.edit()
            editor.putBoolean(IS_BEEP_TIMER_ENABLED, isEnabled)
            editor.commit()
        }

        fun getBeepTimerStatus(): Boolean {
            return preferenceManager.getBoolean(IS_BEEP_TIMER_ENABLED, false)
        }

        fun setExpertPremiumUser(premium: Boolean) {
            val editor = preferenceManager.edit()
            editor.putBoolean(IS_EXPERT_PREMIUM_USER, premium)
            editor.commit()
        }

        fun getExpertPremiumUser(): Boolean {
            if(::preferenceManager.isInitialized.not())
                Utils.context?.let { initServicePref(it) }
            return preferenceManager.getBoolean(IS_EXPERT_PREMIUM_USER, false)
        }

        fun voipServiceUsed() {
            if(getVoipServiceStatus().not()) {
                val editor = preferenceManager.edit()
                editor.putBoolean(IS_VOIP_SERVICE_USED, true)
                editor.commit()
            }
        }

        fun getVoipServiceStatus(): Boolean {
            if(::preferenceManager.isInitialized.not())
                Utils.context?.let { initServicePref(it) }
            return preferenceManager.getBoolean(IS_VOIP_SERVICE_USED, false)
        }
    }
}