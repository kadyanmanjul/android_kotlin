package com.joshtalks.badebhaiya.liveroom.service.util

import android.app.Service
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.JoshSkillExecutors

class WebRtcAudioManager(context: Context) {
    private var playing = false
    private var loaded = false

    private var connectedSoundId = 0
    private var disconnectedSoundId = 0

    private var playingCalled = false
    private val pattern = longArrayOf(100, 200, 400)

    private var soundPool: SoundPool
    private var vibrator: Vibrator
    private var toneThread: ToneThread? = null

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
        toneThread = ToneThread("EngageThread", context)
        toneThread?.start()

        connectedSoundId = soundPool.load(context, R.raw.join_call, 1)
        disconnectedSoundId = soundPool.load(context, R.raw.end_call, 1)
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
            stopConnectTone()
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
            stopConnectTone()
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

    fun startConnectTone() {
        toneThread?.startBusyTone()
    }

    fun stopConnectTone() {
        toneThread?.stopBusyTone()
    }

    fun quitEverything() {
        try {
            toneThread?.quitSafely()
            soundPool.release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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