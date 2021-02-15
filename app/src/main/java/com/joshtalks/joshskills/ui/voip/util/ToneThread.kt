package com.joshtalks.joshskills.ui.voip.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.HandlerThread

class ToneThread(name: String) : HandlerThread(name) {
    private var mHandler: Handler? = null
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var canStop = true
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mHandler = Handler(looper) {
            if (canStop) {
                mHandler?.removeCallbacksAndMessages(null)
            } else {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                mHandler?.sendEmptyMessageDelayed(0, 1500)
            }
            true
        }
    }

    fun startBusyTone() {
        canStop = false
        mHandler?.sendEmptyMessage(0)
    }

    fun stopBusyTone() {
        canStop = true
        mHandler?.sendEmptyMessage(0)
    }
}
