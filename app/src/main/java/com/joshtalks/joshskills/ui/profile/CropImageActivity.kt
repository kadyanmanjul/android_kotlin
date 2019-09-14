package com.joshtalks.joshskills.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityCropImageBinding
import com.isseiaoki.simplecropview.CropImageView
import kotlinx.android.synthetic.main.activity_crop_image.*
import java.io.File
import com.isseiaoki.simplecropview.callback.LoadCallback
import com.isseiaoki.simplecropview.callback.CropCallback
import com.isseiaoki.simplecropview.callback.SaveCallback
import com.joshtalks.joshskills.core.io.AppDirectory
import android.content.Intent
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val SOURCE_IMAGE = "source"

class CropImageActivity : BaseActivity() {
    private lateinit var cropImageBinding: ActivityCropImageBinding
    private lateinit var filePath: String
    private val mCompressFormat = Bitmap.CompressFormat.JPEG
    private var mSourceUri: Uri? = null
    private var mSaveUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        cropImageBinding = DataBindingUtil.setContentView(this, R.layout.activity_crop_image)
        cropImageBinding.handler = this
        crop_image_view.setCropMode(CropImageView.CropMode.SQUARE);


        if (intent != null && intent.hasExtra(SOURCE_IMAGE)) {
            filePath = intent.getStringExtra(SOURCE_IMAGE)
            mSourceUri = Uri.fromFile(File(filePath))
            crop_image_view.load(mSourceUri)
                .useThumbnail(true)
                .execute(mLoadCallback);


        }
    }

    fun cropImage() {
        FullScreenProgressDialog.display(this)
        CoroutineScope(Dispatchers.IO).launch {
            mSaveUri = Uri.fromFile(AppDirectory.imageSentFile())
            crop_image_view.crop(mSourceUri)
                .execute(object : CropCallback {
                    override fun onSuccess(cropped: Bitmap) {
                        crop_image_view.save(cropped)
                            .execute(mSaveUri, mSaveCallback)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })

        }
    }

    fun rotateLeft() {
        crop_image_view.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D); // rotate clockwise by 90 degrees

    }

    fun rotateRight() {
        crop_image_view.rotateImage(CropImageView.RotateDegrees.ROTATE_90D); // rotate clockwise by 90 degrees

    }

    fun cancel() {
        var resultIntent = Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }


    private val mSaveCallback = object : SaveCallback {
        override fun onSuccess(outputUri: Uri) {
            val resultIntent = Intent();
            resultIntent.putExtra(SOURCE_IMAGE, outputUri.path);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }
    private val mLoadCallback = object : LoadCallback {
        override fun onSuccess() {}

        override fun onError(e: Throwable) {}
    }
}
