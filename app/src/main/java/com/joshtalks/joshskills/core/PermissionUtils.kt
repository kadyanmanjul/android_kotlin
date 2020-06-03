package com.joshtalks.joshskills.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

enum class PermissionAction(val action: String) {
    ALLOW("ALLOW"),
    CANCEL("CANCEL"),
    DO_NOT_ASK_AGAIN("DO_NOT_ASK_AGAIN")
}

object PermissionUtils {

    fun storageReadAndWritePermission(
        activity: Activity?,
        multiplePermissionsListener: MultiplePermissionsListener
    ) {
        Dexter.withContext(activity)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(multiplePermissionsListener).check()
    }


    fun audioRecordStorageReadAndWritePermission(
        activity: Activity?,
        multiplePermissionsListener: MultiplePermissionsListener
    ) {
        Dexter.withContext(activity)
            .withPermissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(multiplePermissionsListener).check()
    }


    fun isStoragePermissionEnabled(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAudioAndStoragePermissionEnable(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) +
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun permissionPermanentlyDeniedDialog(
        activity: Activity,
        message: Int = R.string.storage_permission_message
    ) {
        MaterialDialog(activity).show {
            message(message)
            positiveButton(R.string.settings) {
                openSettings(activity)

            }
            negativeButton(R.string.not_now)
        }
    }

    private fun openSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }


    fun checkPermissionForAudioRecord(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun cameraStoragePermissionPermanentlyDeniedDialog(
        activity: Activity,
        message: Int = R.string.camera_permission_message
    ) {
        MaterialDialog(activity).show {
            message(message)
            positiveButton(R.string.settings) {
                openSettings(activity)

            }
            negativeButton(R.string.not_now)
        }
    }


    fun cameraRecordStorageReadAndWritePermission(
        activity: Activity?,
        multiplePermissionsListener: MultiplePermissionsListener
    ) {
        Dexter.withContext(activity)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO

            )
            .withListener(multiplePermissionsListener).check()
    }
}