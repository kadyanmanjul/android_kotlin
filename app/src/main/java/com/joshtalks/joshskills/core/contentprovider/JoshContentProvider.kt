package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.voip.voipLog

private const val TAG = "JoshContentProvider"
class JoshContentProvider : ContentProvider() {

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
            START_CALL_TIME_URI -> {
                val startTime = VoipPref.getStartTimeStamp()
                val cursor = MatrixCursor(arrayOf(START_CALL_TIME_COLUMN))
                cursor.addRow(arrayOf(startTime))
                Log.d(TAG, "query: Timestamp --> $startTime")
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
                Log.d(TAG, "insert: timestamp --> $startCallTimestamp")
                if(startCallTimestamp == 0L)
                    VoipPref.updateCallDetails(startCallTimestamp)
                else {
                    val remoteUserName = values?.getAsString(REMOTE_USER_NAME) ?: ""
                    val remoteUserImage = values?.getAsString(REMOTE_USER_IMAGE)
                    val remoteAgoraId = values?.getAsInteger(REMOTE_USER_AGORA_ID) ?: -1
                    val callId = values?.getAsInteger(CALL_ID) ?: -1
                    val callType = values?.getAsInteger(CALL_TYPE) ?: -1
                    val topicName = values?.getAsString(TOPIC_NAME) ?: ""
                    val channelName = values?.getAsString(CHANNEL_NAME) ?: ""
                    val currentUserAgoraId = values?.getAsInteger(CURRENT_USER_AGORA_ID) ?: -1
                    VoipPref.updateCallDetails(
                        timestamp = startCallTimestamp,
                        remoteUserImage = remoteUserImage,
                        remoteUserName = remoteUserName,
                        remoteUserAgoraId = remoteAgoraId,
                        callId = callId,
                        callType = callType,
                        currentUserAgoraId = currentUserAgoraId,
                        channelName = channelName,
                        topicName = topicName
                    )
                }

            }
            CALL_DISCONNECTED_URI -> {
                VoipPref.updateLastCallDetails()
                VoipPref.updateCallDetails(0)
            }
            VOIP_STATE_URI -> {
                val state = values?.getAsInteger(VOIP_STATE) ?: 0
                VoipPref.updateVoipState(state)
            }
            INCOMING_CALL_URI -> {
                val callId = values?.getAsInteger(CALL_ID) ?: -1
                val callType = values?.getAsInteger(CALL_TYPE) ?: -1
                Log.d(TAG, "insert: timestamp --> $callId ..... $callType")
                VoipPref.updateIncomingCallData(callId, callType)
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