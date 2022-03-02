package com.joshtalks.joshskills.ui.voip.sound_manager

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.core.AppObjectController
import java.util.*

class SoundManager(private val soundType: Int, private val duration: Long = 0) {

    private var pattern = longArrayOf(0, 1500, 1000)
    private val context: Context = AppObjectController.joshApplication
    private var defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, soundType)

    companion object {
        private var defaultRingtone: Ringtone? = null
        private var vibrator: Vibrator? = null
    }

    private fun getRingtoneInstance(): Ringtone? {
        if (defaultRingtone == null) {
            vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
            defaultRingtone = RingtoneManager.getRingtone(context, defaultRingtoneUri)
        }
        return defaultRingtone
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun playSound() {
        gainAudioFocus()
        val ringtone = getRingtoneInstance()
        if(soundType== SOUND_TYPE_RINGTONE) {
            ringtone?.isLooping = true
        }
        ringtone?.play()
        if(duration>0 && soundType== SOUND_TYPE_RINGTONE){
            stopSoundTimer(ringtone)
        }
        vibrateDevice(context)
    }

    fun stopSound() {
        getRingtoneInstance()?.stop()
        vibrator?.cancel()
    }

    private fun stopSoundTimer(ringtone: Ringtone?) {
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                ringtone?.stop()
                vibrator?.cancel()
            }
        }
        val timer = Timer()
        timer.schedule(task, duration)
    }

    private fun vibrateDevice(context: Context) {
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
        val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
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
            mAudioManager!!.requestAudioFocus(
                { },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }
}


