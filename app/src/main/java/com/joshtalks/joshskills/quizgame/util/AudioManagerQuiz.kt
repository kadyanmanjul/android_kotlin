package com.joshtalks.joshskills.quizgame.util

import android.app.Application
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class AudioManagerQuiz() {
    private var recordingJob: CoroutineScope? = null
    private var mPlayer: MediaPlayer?=null

    fun startPlaying(application:Application, soundFile:Int){
        destroyCurrentScope()
        recordingJob = CoroutineScope(Dispatchers.IO)
        recordingJob?.launch {
            try {
                mPlayer = MediaPlayer.create(application, soundFile)
                mPlayer?.start()
            }catch (ex:Exception){
                ex.printStackTrace()
            }
        }
    }

    private fun destroyCurrentScope() {
        try {
            recordingJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlaying() {
        destroyCurrentScope()
        try {
            mPlayer?.stop()
            mPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mPlayer = null
    }

    companion object {
        val audioRecording = AudioManagerQuiz()
    }
}