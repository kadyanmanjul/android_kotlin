package com.joshtalks.joshskills

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class GenericFileProvider : FileProvider() {

    companion object {
        private const val AUTHORITY = "com.joshtalks.joshskills.provider"
        fun getUriForFile(context: Context, file: File): Uri = getUriForFile(context, AUTHORITY, file)
        fun getUriForFile(context: Context, path: String): Uri = getUriForFile(context, AUTHORITY, File(path))
    }
}