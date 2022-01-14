package com.joshtalks.joshskills.quizgame.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import com.joshtalks.joshskills.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber


class AudioManagerQuiz() {
    private var recordingJob: CoroutineScope? = null
     var mPlayer: MediaPlayer?=null
   // private var  speed = 1.2f

    fun startPlaying(application:Context, soundFile:Int,isLoop:Boolean){
        destroyCurrentScope()
        recordingJob = CoroutineScope(Dispatchers.IO)
        recordingJob?.launch {
            try {
                mPlayer = MediaPlayer.create(application, soundFile)
                mPlayer?.setVolume(0.3f, 0.3f)
                mPlayer?.start()
                if (isLoop)
                  mPlayer?.isLooping = true
            }catch (ex:Exception){
                ex.printStackTrace()
            }
        }
    }

    fun tickPlaying(application:Context){
            try {
                val mediaPlayer: MediaPlayer = MediaPlayer.create(
                    application,
                    R.raw.click
                )
                mediaPlayer.setVolume(0.6f,0.6f)
                mediaPlayer.setOnCompletionListener { mediaPlayer ->
                    mediaPlayer.reset()
                    mediaPlayer.release()
                }
                mediaPlayer.start()
            } catch (ex: Exception) {
                Timber.d(ex)
            }
    }

    fun isPlaying(): Boolean {
        return mPlayer?.isPlaying ?: false
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

    fun checkMusicPlayingOrNot(context: Context) : Boolean{
        val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.isMusicActive
    }

    companion object {
        val audioRecording = AudioManagerQuiz()
    }
}