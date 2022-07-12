package com.joshtalks.joshskills.util

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.server.RequestEngage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ScreenshotDetector(private val context: Context) {

    private var contentObserver: ContentObserver? = null
    private var lastScreenshotUri: Uri = Uri.EMPTY

    fun start() {
        if (contentObserver == null) {
            contentObserver = context.contentResolver.registerObserver()
        }
    }

    fun stop() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
    }

    private fun queryScreenshots(uri: Uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryRelativeDataColumn(uri)
            } else {
                queryDataColumn(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun queryDataColumn(uri: Uri) {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                if (path.contains("screenshot", true)) {
                    savePendingTaskToDatabase(path)
                }
            }
        }
    }

    @Throws(Exception::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun queryRelativeDataColumn(uri: Uri) {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val relativePathColumn =
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val displayNameColumn =
                cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(displayNameColumn)
                val relativePath = cursor.getString(relativePathColumn)
                if (name.contains("screenshot", true) or
                    relativePath.contains("screenshot", true)
                ) {
                    savePendingTaskToDatabase(
                        File(
                            Environment.getExternalStorageDirectory(),
                            "$relativePath/$name"
                        ).absolutePath
                    )
                }
            }
        }
    }

    private fun savePendingTaskToDatabase(filePath: String) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val activityName = AppObjectController.currentActivityClass ?: ""
            AppObjectController.appDatabase.pendingTaskDao().insertPendingTask(
                PendingTaskModel(RequestEngage().apply {
                    this.localPath = filePath
                    this.text = activityName
                }, PendingTask.APP_SCREENSHOT)
            )
        }
    }

    private fun ContentResolver.registerObserver(): ContentObserver {
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null && uri != lastScreenshotUri) {
                    lastScreenshotUri = uri
                    queryScreenshots(uri)
                }
            }
        }
        registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
        return contentObserver
    }
}