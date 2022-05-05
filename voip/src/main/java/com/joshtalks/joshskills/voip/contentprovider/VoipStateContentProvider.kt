package com.joshtalks.joshskills.voip.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.voip.constant.CURRENT_PSTN_STATE
import com.joshtalks.joshskills.voip.constant.CURRENT_VOIP_STATE
import com.joshtalks.joshskills.voip.constant.PSTN_STATE_PATH
import com.joshtalks.joshskills.voip.constant.VOIP_STATE_PATH
import com.joshtalks.joshskills.voip.data.local.PrefManager

private const val TAG = "VoipStateContentProvide"
class VoipStateContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { PrefManager.initServicePref(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when(uri.path){
            VOIP_STATE_PATH ->{
                val currentState = PrefManager.getVoipState()
                Log.d(TAG, "query: State -> $currentState")
                val cursor = MatrixCursor(arrayOf(CURRENT_VOIP_STATE))
                cursor.addRow(arrayOf(currentState.ordinal))
                return cursor
            }
            PSTN_STATE_PATH->{
                val currentState = PrefManager.getPstnState()
                Log.d(TAG, "query: PSTN State -> $currentState")
                val cursor = MatrixCursor(arrayOf(CURRENT_PSTN_STATE))
                cursor.addRow(arrayOf(currentState))
                return cursor
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
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