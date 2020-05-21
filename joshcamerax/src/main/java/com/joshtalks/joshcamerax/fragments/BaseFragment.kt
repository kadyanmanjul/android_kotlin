package com.joshtalks.joshcamerax.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshcamerax.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**Parent class of all the fragments in this project*/
abstract class BaseFragment<B : ViewDataBinding>(private val fragmentLayout: Int) : Fragment() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    protected lateinit var binding: B // Generic ViewBinding of the subclasses
    protected lateinit var outputDirectory: File // The Folder where all the files will be stored

    val IMAGE_SELECT = 124
    val VIDEO_SELECT = 125



    // The permissions we need for the app to work properly
    private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the fragment is retained, the lifecycle does not get restarted it on config change
        retainInstance = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Adding an option to handle the back press in fragment
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })

        // Init the output folder
        outputDirectory = File(
            requireContext().getExternalFilesDir(
                Environment.DIRECTORY_DCIM
        )?.absolutePath)
        /*outputDirectory = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath
                        ?: requireContext().externalMediaDirs.first().absolutePath
        )*/

        // Create a binding instance
        binding = DataBindingUtil.inflate(inflater, fragmentLayout, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Each time the app comes to foreground we will check for the permissions
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(), permissions,
                    REQUEST_CODE_PERMISSIONS
            )
        }
    }

    @SuppressLint("WrongConstant")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                onPermissionGranted()
            } else {
                view?.let { v ->
                    Snackbar.make(v, R.string.message_no_permissions, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.label_ok) { ActivityCompat.finishAffinity(requireActivity()) }
                            .show()
                }
            }
        }
    }

    // Check for the permissions
    protected fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    open fun onPermissionGranted() = Unit // a function which will be called after the permission check

    abstract fun onBackPressed() // an abstract function which will be called on the Back button press

    open fun writeImage(
        bitmap: Bitmap, path: String?, quality: Int, newWidth: Int,
        newHeight: Int
    ): File? {
        var mBitmap = bitmap
        var mNewWidth = newWidth
        var mNewHeight = newHeight
        val dir =
            File(Environment.getExternalStorageDirectory(), path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        outputDirectory = File(
            dir, "IMG_"
                    + SimpleDateFormat(
                "yyyyMMdd_HHmmSS",
                Locale.ENGLISH
            ).format(Date())
                    + ".jpg"
        )
        if (outputDirectory.exists()) {
            outputDirectory.delete()
        }
        if (mNewWidth == 0 && mNewHeight == 0) {
            mNewWidth = mBitmap.width / 2
            mNewHeight = mBitmap.height / 2
        }
        mBitmap = getResizedBitmap(mBitmap, mNewWidth, mNewHeight)
        try {
            val fos = FileOutputStream(outputDirectory.path)
            mBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            // fos.write(jpeg);
            fos.close()
        } catch (e: Exception) {
            Log.e("PictureDemo", "Exception in photoCallback", e)
        }
        return outputDirectory
    }

    open fun getResizedBitmap(
        bm: Bitmap,
        newWidth: Int,
        newHeight: Int
    ): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)
        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
        return resizedBitmap.copy(Bitmap.Config.RGB_565, false)
    }


}