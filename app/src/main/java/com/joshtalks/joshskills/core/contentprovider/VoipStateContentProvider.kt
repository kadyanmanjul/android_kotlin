package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.ui.call.data.local.VoipPref

class VoipStateContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { VoipPref.initVoipPref(it) }
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
                val currentState = VoipPref.getCurrentVoipState()
                val cursor = MatrixCursor(arrayOf(CURRENT_VOIP_STATE))
                cursor.addRow(arrayOf(currentState))
                return cursor
            }
            VOIP_STATE_STACK_PATH -> {
                val currentState = VoipPref.getCurrentVoipStateStack()
                val cursor = MatrixCursor(arrayOf(CURRENT_VOIP_STATE_STACKS))
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
        when(uri.path){
            VOIP_STATE_PATH ->{
               val currentVoipState = values?.getAsString(CURRENT_VOIP_STATE)
                VoipPref.updateCurrentVoipState(currentVoipState)
            }
            VOIP_STATE_STACK_PATH -> {
                val currentVoipStateStack = values?.get(CURRENT_VOIP_STATE_STACKS)
                VoipPref.updateCurrentVoipStateStack(currentVoipStateStack)
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