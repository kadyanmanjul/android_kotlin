package com.joshtalks.joshcamerax

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshcamerax.utils.Utility

class JoshCameraActivity : AppCompatActivity() {


    companion object {
        fun startJoshCameraxActivity(context:FragmentActivity, options: Options) {
            Intent(context, JoshCameraActivity::class.java).apply {
                putExtra(OPTIONS, options)
            }.run {
                context.startActivityForResult(this,options.requestCode)
            }
        }

        private const val OPTIONS = "options"
        var IMAGE_RESULTS = "image_results"
        var VIDEO_RESULTS = "video_results"

        val IMAGE_SELECT = 124
        val VIDEO_SELECT = 125
    }


    private val TAG = "JoshCameraActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Utility.setupStatusBarHidden(this)
        Utility.hideStatusBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        Utility.setupStatusBarHidden(this)
        Utility.hideStatusBar(this)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult() called with: requestCode = $requestCode, resultCode = $resultCode, data = $data"
        )
        if (data != null) {
            if (requestCode == IMAGE_SELECT) {
                val returnValue =
                    data.getStringArrayListExtra(IMAGE_RESULTS)
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra(IMAGE_RESULTS, returnValue)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else if (requestCode == VIDEO_SELECT) {
                val returnValue = data.getStringExtra("video_uri")
                Log.d(
                    TAG,
                    "onActivityResult() called with: requestCode = $requestCode, resultCode = $resultCode, returnValue = $returnValue  returnValue $"
                )
                val resultIntent = Intent()
                resultIntent.putExtra(VIDEO_RESULTS, returnValue)
                setResult(Activity.RESULT_OK, resultIntent)

                finish()
            }
        }
    }

}
