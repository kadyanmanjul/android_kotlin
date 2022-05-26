package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.video_player.DURATION
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.LEAVING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "JoshContentProvider"
class JoshContentProvider : ContentProvider() {
    val mutex = Mutex(false)

    override fun onCreate(): Boolean {
        voipLog?.log("On Create Content Provider $context")
        context?.let { VoipPref.initVoipPref(it) }
        Log.d(TAG, "onCreate: ")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when(uri.path) {
            API_HEADER -> {
                val apiHeader = ApiHeader(
                    token = "JWT " + PrefManager.getStringValue(API_TOKEN),
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE.toString(),
                    userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
                    acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
                )

                val cursor = MatrixCursor(arrayOf(AUTHORIZATION, APP_VERSION_NAME, APP_VERSION_CODE, APP_USER_AGENT, APP_ACCEPT_LANGUAGE))
                cursor.addRow(arrayOf(apiHeader.token, apiHeader.versionName, apiHeader.versionCode, apiHeader.userAgent, apiHeader.acceptLanguage))
                Log.d(TAG, "query: Api Header --> $apiHeader")
                return cursor
            }
            MENTOR_ID -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_ID_COLUMN))
                cursor.addRow(arrayOf(Mentor.getInstance().getId()))
                return cursor
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? { return null }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        voipLog?.log("INSERT")
        when(uri.path) {
            START_CALL_TIME_URI -> {
                val startCallTimestamp = values?.getAsLong(CALL_START_TIME) ?: 0L
                VoipPref.updateCurrentCallStartTime(startCallTimestamp)
            }
            CALL_DISCONNECTED_URI -> {
                val duration = values?.getAsLong(CALL_DURATION) ?: 0L
                val remoteUserName = values?.getAsString(REMOTE_USER_NAME) ?: ""
                val remoteUserImage = values?.getAsString(REMOTE_USER_IMAGE)
                val remoteAgoraId = values?.getAsInteger(REMOTE_USER_AGORA_ID) ?: -1
                val callId = values?.getAsInteger(CALL_ID) ?: -1
                val callType = values?.getAsInteger(CALL_TYPE) ?: -1
                val topicName = values?.getAsString(TOPIC_NAME) ?: ""
                val channelName = values?.getAsString(CHANNEL_NAME) ?: ""
                val currentUserAgoraId = values?.getAsInteger(CURRENT_USER_AGORA_ID) ?: -1
                val fppFlag = values?.getAsString(FPP_SHOW_FLAG) ?: "true"

                VoipPref.updateLastCallDetails(
                    duration = duration,
                    remoteUserImage = remoteUserImage,
                    remoteUserName = remoteUserName,
                    remoteUserAgoraId = remoteAgoraId,
                    callId = callId,
                    callType = callType,
                    localUserAgoraId = currentUserAgoraId,
                    channelName = channelName,
                    topicName = topicName,
                    showFpp = fppFlag
                )
            }
        }
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}