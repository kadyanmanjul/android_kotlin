package com.joshtalks.appcamera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.appcamera.video_trimmer.interfaces.VideoTrimmingListener
import kotlinx.android.synthetic.main.activity_trimmer.*
import java.io.File

const val VIDEO_URI = "video_uri"
const val DEST_VIDEO_FILE = "dest_video_file"

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

        videoTrimmerView.setMaxDurationInMs(999999999)
        videoTrimmerView.setOnK4LVideoListener(this)
        videoTrimmerView.setDestinationFile(destFile)
        videoTrimmerView.setVideoURI(inputVideoUri)
        videoTrimmerView.setVideoInformationVisibility(true)
    }

    override fun onTrimStarted() {
        trimmingProgressView.visibility = View.VISIBLE
    }

    override fun onFinishedTrimming(uri: Uri?) {
        trimmingProgressView.visibility = View.GONE
        if (uri == null) {
            var resultIntent = Intent();
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
            Log.e("Error", "failed trimming")
        } else {
            var resultIntent = Intent();
            resultIntent.putExtra(VIDEO_URI, uri.path);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
        finish()
    }

    override fun onErrorWhileViewingVideo(what: Int, extra: Int) {
        trimmingProgressView.visibility = View.GONE
        Log.e("Error", "error while previewing video")
    }

    override fun onVideoPrepared() {
    }


    companion object {
        @JvmStatic
        fun startTrimmerActivity(context: Activity, requestCode:Int, videoUri: Uri, destFile: File) {
            var intent = Intent(context, VideoTrimmerActivity::class.java)
            intent.putExtra(VIDEO_URI, videoUri)
            intent.putExtra(DEST_VIDEO_FILE,destFile)
            context.startActivityForResult(intent,requestCode)

        }

    }
}
