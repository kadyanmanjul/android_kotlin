package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.base.constants.CALL_ID
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.CALL_TYPE
import com.joshtalks.joshskills.base.constants.REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.CALL_DISCONNECTED_URI
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_COLUMN
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_URI
import com.joshtalks.joshskills.base.constants.VOIP_STATE_URI
import com.joshtalks.joshskills.base.constants.VOIP_STATE
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
                return cursor
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? { return null }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        voipLog?.log("INSERT")
        Log.d(TAG, "insert: ")
        when(uri.path) {
            START_CALL_TIME_URI -> {
                val startCallTimestamp = values?.getAsLong(CALL_START_TIME) ?: 0L
                if(startCallTimestamp == 0L)
                    VoipPref.updateCallDetails(startCallTimestamp)
                else {
                    val remoteUserName = values?.getAsString(REMOTE_USER_NAME) ?: ""
                    val remoteUserImage = values?.getAsString(REMOTE_USER_IMAGE)
                    val remoteAgoraId = values?.getAsInteger(REMOTE_USER_AGORA_ID) ?: -1
                    val callId = values?.getAsInteger(CALL_ID) ?: -1
                    val callType = values?.getAsInteger(CALL_TYPE) ?: -1
                    VoipPref.updateCallDetails(
                        timestamp = startCallTimestamp,
                        remoteUserImage = remoteUserImage,
                        remoteUserName = remoteUserName,
                        remoteUserAgoraId = remoteAgoraId,
                        callId = callId,
                        callType = callType
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