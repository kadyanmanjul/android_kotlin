package com.joshtalks.joshskills.ui.voip.util

import android.app.Service
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.JoshSkillExecutors

class WebRtcAudioManager(context: Context) {
    private var playing = false
    private var loaded = false

    private var connectedSoundId = 0
    private var disconnectedSoundId = 0

    private var playingCalled = false
    private val pattern = longArrayOf(100, 200, 400)

    private var soundPool: SoundPool
    private var vibrator: Vibrator
    private var engageCallRinger: EngageCallRinger

    private val maxVolume = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        .getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

    init {
        val att: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(att)
            .build()
        connectedSoundId = soundPool.load(context, R.raw.join_call, 1)
        disconnectedSoundId = soundPool.load(context, R.raw.end_call, 1)
        engageCallRinger = EngageCallRinger(context)
        vibrator = context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        soundPool.setOnLoadCompleteListener { s: SoundPool, _: Int, _: Int ->
            JoshSkillExecutors.BOUNDED.execute {
                loaded = true
                if (playingCalled) {
                    playVibrate()
                    startCommunication()
                    playingCalled = false
                }
            }
        }
    }

    fun startCommunication() {
        try {
            stopRinging()
            if (loaded && !playing) {
                soundPool.play(connectedSoundId, maxVolume, maxVolume, 1, 0, 1.0f)
                playVibrate()
                playing = true
            } else {
                playingCalled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun endCommunication() {
        try {
            stopRinging()
            if (loaded && !playing) {
                soundPool.play(disconnectedSoundId, maxVolume, maxVolume, 1, 0, 1.0f)
                playVibrate()
                playing = true
            } else {
                playingCalled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reconnectCommunication() {
        engageCallRinger.start()
    }

    fun reconnectCommunicationStop() {
        engageCallRinger.stop()
    }


    private fun stopRinging() {
        if (playing) {
            soundPool.stop(disconnectedSoundId)
            playing = false
        }
    }

    private fun playVibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }
}