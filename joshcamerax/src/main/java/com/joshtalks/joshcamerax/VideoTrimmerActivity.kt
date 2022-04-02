package com.joshtalks.joshcamerax

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlFilter
import com.joshtalks.joshcamerax.video_trimmer.interfaces.VideoTrimmingListener
import kotlinx.android.synthetic.main.activity_trimmer.*
import java.io.File


const val VIDEO_URI = "video_uri"
const val DEST_VIDEO_FILE = "dest_video_file"
const val SRC_VIDEO_PATH = "src_video_path"

class VideoTrimmerActivity : AppCompatActivity(), VideoTrimmingListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trimmer)
        val inputVideoUri: Uri? = intent?.getParcelableExtra(VIDEO_URI)
        val destFile = intent?.getSerializableExtra(DEST_VIDEO_FILE) as File
        if (inputVideoUri == null) {
            finish()
            return
        }
        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState  $destFile $inputVideoUri ")
        videoTrimmerView.setMaxDurationInMs(999999999)
        videoTrimmerView.setOnK4LVideoListener(this)
        videoTrimmerView.setDestinationFile(destFile)
        videoTrimmerView.setVideoURI(inputVideoUri)
        videoTrimmerView.setVideoInformationVisibility(true)
        progress_bar.spinSpeed = 0.25f
        progress_bar.barColor = Color.parseColor("#128C7E")
        progress_bar.rimColor = Color.parseColor("#33128C7E")


    }

    override fun onTrimStarting(srcPath: Uri, destPath: File, startTime: Long, endTime: Long) {
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val srcVideoPath = intent?.getStringExtra(SRC_VIDEO_PATH)
            Log.d(
                TAG,
                "onTrimStarting() called with: srcPath = $srcPath, destPath = $destPath, startTime = $startTime, endTime = $endTime"
            )
            Mp4Composer(srcVideoPath, destPath.absolutePath)
                .size(360, 640)
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .filter(GlFilter())
                .mute(false)
                .flipHorizontal(false)
                .flipVertical(false)
                .trim(startTime, endTime)
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                        runOnUiThread {
                            progress_bar.progress = progress.toFloat()
                            //progressWheel.setInstantProgress(float value)
                        }

                        Log.d("TAG", "onProgress = $progress")
                    }

                    override fun onCompleted() {
                        Log.d("TAG", "onCompleted()")
                        deleteVideoFile(srcVideoPath)
                        finishTrimming(destPath.absolutePath)

                    }

                    override fun onCanceled() {
                        finishTrimming(null)
                        deleteVideoFile(srcVideoPath)
                        Log.d("TAG", "onCanceled")
                    }

                    override fun onFailed(exception: Exception) {
                        finishTrimming(null)
                        deleteVideoFile(srcVideoPath)
                        Log.e("TAG", "onFailed()", exception)
                        exception.printStackTrace()
                    }
                })
                .start()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }


    private fun deleteVideoFile(srcVideoPath: String?) {
        srcVideoPath?.let {
            val f0 = File(srcVideoPath)
            if (f0.exists()) {
                val d0 = f0.delete()
                Log.w(TAG, "File deleted: $srcVideoPath/myFile $d0")
            }
        }
    }


    override fun onTrimStarted() {
        trimmingProgressView.visibility = View.VISIBLE
    }

    fun finishTrimming(url: String?) {
        runOnUiThread {
            if (url == null) {
                val resultIntent = Intent()
                setResult(Activity.RESULT_CANCELED, resultIntent)
                finish()
            } else {
                val resultIntent = Intent()
                resultIntent.putExtra(VIDEO_URI, url)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onErrorWhileViewingVideo(what: Int, extra: Int) {
        trimmingProgressView.visibility = View.GONE
        Log.e("Error", "error while previewing video")
    }

    override fun onVideoPrepared() {
    }


    companion object {

        @JvmStatic
        fun startTrimmerActivity(
            context: Activity,
            requestCode: Int,
            videoUri: Uri,
            destFile: File,
            srcPath: String? = null
        ) {
            Log.d(
                TAG,
                "startTrimmerActivity() called with: context = $context, requestCode = $requestCode, videoUri = $videoUri, destFile = $destFile, srcPath = $srcPath"
            )

            //2020-05-13 21:17:31.205 32734-32734/com.joshtalks.joshskills D/VideoTrimmerActivity: startTrimmerActivity() called with:
            // context = com.joshtalks.joshcamerax.MainActivity@8c518b3, requestCode = 125,
            // videoUri = file:///storage/emulated/0/Android/data/com.joshtalks.joshskills/files/DCIM/20200513_211753record.mp4,
            // destFile = /storage/emulated/0/Android/data/com.joshtalks.joshskills/files/DCIM/20200513_211753record.mp4,
            // srcPath = /storage/emulated/0/Android/data/com.joshtalks.joshskills/files/DCIM/20200513_211753record.mp4
            val intent = Intent(context, VideoTrimmerActivity::class.java)
            intent.putExtra(VIDEO_URI, videoUri)
            intent.putExtra(DEST_VIDEO_FILE, destFile)
            intent.putExtra(SRC_VIDEO_PATH, srcPath)
            context.startActivityForResult(intent, requestCode)

        }

        private const val TAG = "VideoTrimmerActivity"

    }
}
