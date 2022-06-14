package com.joshtalks.joshskills.ui.voip.util

import android.content.Context
import android.media.MediaRecorder
import android.view.View
import com.joshtalks.joshskills.core.analytics.ErrorTag
import com.joshtalks.joshskills.core.analytics.LogException
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallRecording {
    private var recorder: ViewRecorder? = null
    private var recordingJob: CoroutineScope? = null

    fun startPlayer(recordFile: File?,context:Context,view: View) {
        destroyCurrentScope()
        recordingJob = CoroutineScope(Dispatchers.IO)
        recordingJob?.launch {
            try {
                recorder = ViewRecorder()
                recorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder?.setVideoFrameRate(5); // 5fps
                recorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                recorder?.setVideoSize(720, 1280);
                recorder?.setVideoEncodingBitRate(2000 * 1000);
                recorder?.setRecordedView(view);
                recorder?.setOutputFile(recordFile?.absolutePath)
                val errorListener =
                    MediaRecorder.OnErrorListener { arg0, arg1, arg2 ->
                        LogException.catchError(
                            ErrorTag.AUDIO_RECORDER,
                            "$arg1 , $arg2"
                        )
                    }
                recorder?.setOnErrorListener(errorListener)
                recorder?.prepare()
                delay(500)
                recorder?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopPlaying() {
        destroyCurrentScope()
        try {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
    }

    fun isRecording() = recorder != null

    private fun destroyCurrentScope() {
        try {
            recordingJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val videoRecorder = CallRecording()
    }
}