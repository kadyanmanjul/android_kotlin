package com.joshtalks.joshskills.util

import android.media.MediaRecorder
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

    fun startPlayer(recordFile: File?) {
        destroyCurrentScope()
        recordingJob = CoroutineScope(Dispatchers.IO)
        recordingJob?.launch {
            try {
                recorder = MediaRecorder()
                recorder?.setAudioChannels(1)
                recorder?.setAudioSamplingRate(16000)
                recorder?.setAudioEncodingBitRate(32000)
                recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
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