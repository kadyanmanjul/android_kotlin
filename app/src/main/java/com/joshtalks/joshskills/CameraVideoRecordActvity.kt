package com.joshtalks.joshskills

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.camerafragment_activity_main.*
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.joshtalks.joshskills.core.io.AppDirectory
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri


class CameraVideoRecordActvity : AppCompatActivity() {

    val FRAGMENT_TAG = "camera"
    private val REQUEST_CAMERA_PERMISSIONS = 931
    private val REQUEST_PREVIEW_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camerafragment_activity_main)
        //if (p0 != null) {
        //}
        findViewById<View>(R.id.record_button1).setOnClickListener {
            camera_view.captureVideo(object : CameraKitEventCallback<CameraKitVideo> {
                override fun callback(p0: CameraKitVideo?) {


                    // ResampleActivity().onCreate(Uri.fromFile( p0?.videoFile))
/*
                    Thread(Runnable {
                        val path=AppDirectory.videoReceivedFile().absolutePath


                        val task = VideoCompress.compressVideoMedium(
                            p0?.videoFile?.absolutePath,
                            path,
                            object : VideoCompress.CompressListener {
                                override fun onStart() {
                                    //Start Compress
                                    Log.e("succe","onStart")

                                }

                                override fun onSuccess() {
                                    //Finish successfully
                                    Log.e("succe","ssucc")
                                }

                                override fun onFail() {
                                    Log.e("succe","onFail")

                                    //Failed
                                }

                                override fun onProgress(percent: Float) {
                                    //Progress
                                    Log.e("succe","onProgress")

                                }
                            })
                    }).start()
*/


/*

                        val filePath =
                            SiliCompressor.with(applicationContext).compressVideo(p0?.videoFile?.absolutePath,path )
*/

                       // Log.e("file", "" + (p0?.videoFile ?: "")+filePath)



                }

            })
            camera_view.postDelayed({ camera_view.stopVideo() }, 1000 * 5)


        }


        camera_view.setFacing(CameraKit.Constants.FACING_BACK);
        camera_view.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_480P);
        camera_view.setVideoBitRate(CameraKit.Constants.VIDEO_QUALITY_480P);

        camera_view.captureVideo()


    }

    override fun onResume() {
        super.onResume()
        camera_view.start()
    }

    override fun onPause() {
        camera_view.stop()
        super.onPause()
    }
}
/*

Manifest.permission.CAMERA,
Manifest.permission.RECORD_AUDIO,
Manifest.permission.WRITE_EXTERNAL_STORAGE,
Manifest.permission.READ_EXTERNAL_STORAGE*/
