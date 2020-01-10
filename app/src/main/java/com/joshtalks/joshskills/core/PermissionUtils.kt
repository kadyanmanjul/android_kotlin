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

object PermissionUtils {

    fun storageReadAndWritePermission(
        activity: Activity?,
        multiplePermissionsListener: MultiplePermissionsListener
    ) {
        Dexter.withActivity(activity)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(multiplePermissionsListener).check()
    }

    fun isStoragePermissionEnable(context: Context):Boolean{
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }



    fun storagePermissionPermanentlyDeniedDialog(activity: Activity) {
        MaterialDialog(activity).show {
            message(R.string.permission_message)
            positiveButton(R.string.ok) {
                openSettings(activity)

            }
            negativeButton(R.string.cancel)
        }
    }

    private fun openSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivityForResult(intent, 101)
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


}