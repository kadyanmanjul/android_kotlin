package com.joshtalks.joshskills.util

import android.media.MediaRecorder
import android.os.Build
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.ErrorTag
import com.joshtalks.joshskills.core.analytics.LogException
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioRecording {
    private var recorder: MediaRecorder? = null
    private var recordingJob: CoroutineScope? = null

    fun startPlayer(recordFile: File? ) {
        destroyCurrentScope()
        recordingJob = CoroutineScope(Dispatchers.IO)
        recordingJob?.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    recorder = MediaRecorder(AppObjectController.joshApplication)
                } else {
                    recorder = MediaRecorder()
                }
                recorder?.setAudioChannels(1)
                recorder?.setAudioSamplingRate(48000)
                recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
    }

    private fun destroyCurrentScope() {
        try {
            recordingJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val audioRecording = AudioRecording()
    }
}