package com.joshtalks.joshskills.ui.voip

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.joshtalks.joshskills.core.AppObjectController


class SoundPoolUtility {

    companion object {
        fun playIncomingCallSound() {
            try {
                val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val r: Ringtone =
                    RingtoneManager.getRingtone(AppObjectController.joshApplication, notification)
                r.play()

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        fun stopIncomingCallSound() {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val r: Ringtone =
                RingtoneManager.getRingtone(AppObjectController.joshApplication, notification)
            r.stop()
        }
    }

    /*   fun playRingtone() {
           val audioManager = mContext.getSystemService(AUDIO_SERVICE) as AudioManager
           if (mPlayer != null && mPlayer!!.isPlaying) stopRingtone()
           // Honour silent mode
           if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
               mPlayer = MediaPlayer()
               mPlayer!!.setAudioStreamType(AudioManager.STREAM_RING)
               try {
                   mPlayer!!.setDataSource(
                       mContext,
                       android.media.RingtoneManager.getActualDefaultRingtoneUri(
                           joshApplication,
                           android.media.RingtoneManager.TYPE_ALARM
                       )
                   )
                   mPlayer!!.prepare()
               } catch (e: Exception) {
                   mPlayer = null
                   return
               }
               mPlayer!!.isLooping = true
               mPlayer!!.start()
           }
       }

       fun stopRingtone() {
           if (mPlayer != null) {
               mPlayer!!.stop()
               mPlayer!!.release()
               mPlayer = null
           }
       }

       companion object {
           val LOG_TAG = RingtoneManager::class.java.simpleName
           private const val SAMPLE_RATE = 16000
           private var instance: RingtoneManager? = null
           fun getInstance(context: Context): RingtoneManager? {
               if (instance == null) instance = RingtoneManager(context)
               return instance
           }

           @Throws(IOException::class)
           private fun readFileToBytes(fd: AssetFileDescriptor, data: ByteArray) {
               val inputStream = fd.createInputStream()
               var bytesRead = 0
               while (bytesRead < data.size) {
                   val res = inputStream.read(data, bytesRead, data.size - bytesRead)
                   if (res == -1) {
                       break
                   }
                   bytesRead += res
               }
           }
       }

       init {
           mContext = context.applicationContext
       }
   }*/

/*
    fun incomingCall(){
        val audioAttrib: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val mSound = SoundPool.Builder().setAudioAttributes(audioAttrib).setMaxStreams(10).build()

        val audioManager: AudioManager = AppObjectController.joshApplication.getSystemService(
            AUDIO_SERVICE
        ) as AudioManager
        val actualVolume = audioManager
            .getStreamVolume(AudioManager.STREAM_MUSIC) as Float
        val maxVolume = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC) as Float
        val volume = actualVolume / maxVolume
        // Is the sound loaded already?
        // Is the sound loaded already?

        if (loaded) {
            mSound.play(soundID, volume, volume, 1, 0, 1f)
        }
    }*/

}