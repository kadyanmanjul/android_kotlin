package com.joshtalks.joshskills.voip.audiomanager

import android.app.Service
import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.voip.Utils
import java.util.*

class SoundManager(
    private val soundType: Int,
    private val duration: Long = 0
) {
    private val applicationContext=Utils.context
    private var pattern = longArrayOf(0, 1500, 1000)
    private var defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(applicationContext, soundType)
    private var ringtonePlayer: MediaPlayer? = null
    private var mPlayer: MediaPlayer? = null
    private var ringingPlay = false
    private var vibrator: Vibrator? = null
    companion object {
        private var vibrator: Vibrator? = null
       private var medialPlayer: MediaPlayer? = null
    }

    private fun getMediaPlayerInstance(): MediaPlayer? {
        if (medialPlayer == null) {
            vibrator = applicationContext?.let { ContextCompat.getSystemService(it, Vibrator::class.java) }
            medialPlayer = MediaPlayer()
            medialPlayer?.isLooping=true
            medialPlayer?.setAudioStreamType(AudioManager.STREAM_RING)
            applicationContext?.let { medialPlayer?.setDataSource(it,defaultRingtoneUri)}
            medialPlayer?.prepare()
        }
        return medialPlayer
    }

    fun playSound() {
        gainAudioFocus()
        medialPlayer= getMediaPlayerInstance()
           medialPlayer?.start()
        if (duration > 0 && soundType == SOUND_TYPE_RINGTONE) {
            stopSoundTimer(medialPlayer)
        }
        vibrateDevice()
    }

    fun stopSound() {
        getMediaPlayerInstance()?.stop()
        vibrator?.cancel()
    }

    private fun stopSoundTimer(mediaPlayer: MediaPlayer?) {
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                mediaPlayer?.stop()
                vibrator?.cancel()
            }
        }
        val timer = Timer()
        timer.schedule(task, duration)
    }

    private fun vibrateDevice() {
        if (soundType == SOUND_TYPE_NOTIFICATION) {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= 26) {
                    it.vibrate(500)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(500)
                }
            }
        } else {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= 26) {
                    it.vibrate(pattern, 0)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
            }
        }

    }

    private fun gainAudioFocus() {
        val mAudioManager =
            applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager!!.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener {
                    }.build()
            )
        } else {
            mAudioManager?.requestAudioFocus(
                { },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }
     fun startRingtoneAndVibration() {
        stopPlaying()
        if (ringingPlay) {
            return
        }
        val am = applicationContext?.getSystemService(Service.AUDIO_SERVICE) as AudioManager
        val needRing = am.ringerMode != AudioManager.RINGER_MODE_SILENT
        if (needRing) {
            val att: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            ringtonePlayer = MediaPlayer()
            ringtonePlayer?.setOnPreparedListener { mediaPlayer ->
                try {
                    ringtonePlayer?.start()
                    ringingPlay = true
                } catch (ex: IllegalStateException) {}
            }
            ringtonePlayer?.isLooping = true
            ringtonePlayer?.setAudioAttributes(att)
            ringtonePlayer?.setAudioStreamType(AudioManager.STREAM_RING)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
                )
            } else {
                am.requestAudioFocus({ }, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN)
            }

            try {
                val notificationUri: String =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
                ringtonePlayer?.setDataSource(applicationContext, Uri.parse(notificationUri))
                ringtonePlayer?.prepareAsync()
                ringingPlay = true
            } catch (e: java.lang.Exception) {
                if (ringtonePlayer != null) {
                    ringtonePlayer?.release()
                    ringtonePlayer = null
                    ringingPlay = false
                }
            }

            if ((am.ringerMode == AudioManager.RINGER_MODE_VIBRATE || am.ringerMode == AudioManager.RINGER_MODE_NORMAL) || am.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrator = applicationContext?.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(100, 250, 500, 750, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
        }
    }

    fun stopPlaying() {
        try {
            mPlayer?.run {
                stop()
                release()
                mPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


