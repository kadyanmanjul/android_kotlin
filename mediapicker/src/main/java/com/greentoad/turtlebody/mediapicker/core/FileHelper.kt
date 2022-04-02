package com.greentoad.turtlebody.mediapicker.core

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.greentoad.turtlebody.mediapicker.util.UtilsFile
import java.io.FileNotFoundException

/**
 * Created by WANGSUN on 17-Apr-19.
 */
object FileHelper {

    fun isFileExist(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        val path = UtilsFile.getFilePath(context, uri)

        if (TextUtils.isEmpty(path)) {
            return false
        }
        //return  true

        return try {
            var inputStream = context.contentResolver.openInputStream(uri)
            true
        } catch (e: FileNotFoundException) {
            false
        }
        catch (e: Exception) {
            false
        }
    }
}