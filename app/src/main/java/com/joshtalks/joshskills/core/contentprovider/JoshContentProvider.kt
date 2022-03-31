package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.base.constants.API_HEADER
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.UPDATE_START_CALL_TIME
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.voip.voipLog

private const val TAG = "JoshContentProvider"
class JoshContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        voipLog?.log("On Create Content Provider")
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
        return null
    }

    override fun getType(uri: Uri): String? { return null }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        voipLog?.log("INSERT")
        Log.d(TAG, "insert: ")
        when(uri.path) {
            UPDATE_START_CALL_TIME -> {
                val startCallTimestamp = values?.getAsLong(CALL_START_TIME) ?: 0L
                CallBar.update(startCallTimestamp)
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