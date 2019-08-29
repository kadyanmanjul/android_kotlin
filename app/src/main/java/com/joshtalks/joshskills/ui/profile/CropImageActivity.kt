package com.joshtalks.joshskills.ui.profile

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val SOURCE_IMAGE = "source"

class CropImageActivity : CoreJoshActivity() {
    private lateinit var cropImageBinding: ActivityCropImageBinding
    private lateinit var filePath: String

    private val KEY_FRAME_RECT = "FrameRect"

    private val mCompressFormat = Bitmap.CompressFormat.JPEG
    //private var mFrameRect: RectF? = null
    private var mSourceUri: Uri? = null

    private var mSaveUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropImageBinding = DataBindingUtil.setContentView(this, R.layout.activity_crop_image)
        cropImageBinding.handler = this
       crop_image_view.setCropMode(CropImageView.CropMode.CIRCLE);


        if (intent != null && intent.hasExtra(SOURCE_IMAGE)) {
            filePath = intent.getStringExtra(SOURCE_IMAGE)
            crop_image_view.setDebug(true);

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

    private val mSaveCallback = object : SaveCallback {
        override fun onSuccess(outputUri: Uri) {
            Log.e("file",outputUri.path)
            var resultIntent =  Intent();
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

    /* // Handle button event /////////////////////////////////////////////////////////////////////////

     private val btnListener = object : View.OnClickListener() {
         fun onClick(v: View) {
             when (v.getId()) {
                 R.id.buttonDone -> BasicFragmentPermissionsDispatcher.cropImageWithCheck(this@BasicFragment)
                 R.id.buttonFitImage -> mCropView.setCropMode(CropImageView.CropMode.FIT_IMAGE)
                 R.id.button1_1 -> mCropView.setCropMode(CropImageView.CropMode.SQUARE)
                 R.id.button3_4 -> mCropView.setCropMode(CropImageView.CropMode.RATIO_3_4)
                 R.id.button4_3 -> mCropView.setCropMode(CropImageView.CropMode.RATIO_4_3)
                 R.id.button9_16 -> mCropView.setCropMode(CropImageView.CropMode.RATIO_9_16)
                 R.id.button16_9 -> mCropView.setCropMode(CropImageView.CropMode.RATIO_16_9)
                 R.id.buttonCustom -> mCropView.setCustomRatio(7, 5)
                 R.id.buttonFree -> mCropView.setCropMode(CropImageView.CropMode.FREE)
                 R.id.buttonCircle -> mCropView.setCropMode(CropImageView.CropMode.CIRCLE)
                 R.id.buttonShowCircleButCropAsSquare -> mCropView.setCropMode(CropImageView.CropMode.CIRCLE_SQUARE)
                 R.id.buttonRotateLeft -> mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D)
                 R.id.buttonRotateRight -> mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D)
                 R.id.buttonPickImage -> BasicFragmentPermissionsDispatcher.pickImageWithCheck(this@BasicFragment)
             }
         }
     }*/

    fun getUriFromDrawableResId(context: Context, drawableResId: Int): Uri {
        val builder = StringBuilder().append(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .append("://")
            .append(context.getResources().getResourcePackageName(drawableResId))
            .append("/")
            .append(context.getResources().getResourceTypeName(drawableResId))
            .append("/")
            .append(context.getResources().getResourceEntryName(drawableResId))
        return Uri.parse(builder.toString())
    }
}
