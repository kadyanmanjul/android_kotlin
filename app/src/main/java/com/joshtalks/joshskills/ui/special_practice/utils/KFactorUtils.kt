package com.joshtalks.joshskills.ui.special_practice.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.MediaStore
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.util.getBitMapFromView
import com.joshtalks.joshskills.util.toFile
import com.joshtalks.joshskills.util.uriToFile
import java.util.concurrent.Executor

object KFactorUtils {
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var name: String? = EMPTY
    private var currentRecording: Recording? = null
    private var timer: CountDownTimer? = null

    suspend fun bindCaptureUseCase(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        previewSurface: Preview.SurfaceProvider
    ): VideoCapture<Recorder> {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).await()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build().apply {
                    setSurfaceProvider(previewSurface)
                }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    videoCapture,
                    preview
                )
            } catch (exc: Exception) {
            }
        } catch (ex: Exception) {
        }
        return videoCapture
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        context: Context,
        name: String,
        mainThreadExecutor: Executor,
        captureListener: Consumer<VideoRecordEvent>
    ): Recording? {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
            }
            val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

            // configure Recorder and Start recording to the mediaStoreOutput.
            currentRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .apply { withAudioEnabled() }
                .start(mainThreadExecutor, captureListener)
        } catch (ex: Exception) {}
        return currentRecording
    }

    fun getTime(view: ImageButton): CountDownTimer? {
        try {
            timer = object : CountDownTimer(30000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    currentRecording?.stop()
                    view.setBackgroundResource(R.drawable.ic_ellipse_50__2_)
                    view.setImageResource(R.drawable.ic_rectangle_stop)
                }
            }
        } catch (ex: Exception) {}
        return timer
    }

    fun updateUI(event: VideoRecordEvent,chronometer: Chronometer) {
        when (event) {
            is VideoRecordEvent.Start -> {
                chronometer.visibility = View.VISIBLE
                chronometer.base = SystemClock.elapsedRealtime()
                chronometer.start()
            }
            is VideoRecordEvent.Finalize -> {
                chronometer.stop()
                chronometer.visibility = View.INVISIBLE
            }
        }
    }
    fun changeUIAccordingToState(view:ImageButton,recordingState:VideoRecordEvent) {
        when (recordingState) {
            is VideoRecordEvent.Start -> {
                timer?.onFinish()
                currentRecording?.stop()
                setBtnBackgroundResources(view)
            }
            is VideoRecordEvent.Pause -> {
                currentRecording?.resume()
                view.setImageDrawable(null)
            }
            else -> throw IllegalStateException("recordingState in unknown state")
        }
    }

    fun setBtnBackgroundResources(view: ImageButton) {
        view.setBackgroundResource(R.drawable.ic_ellipse_50__2_)
        view.setImageResource(R.drawable.ic_rectangle_stop)
    }
}