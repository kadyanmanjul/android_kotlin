package com.joshtalks.badebhaiya.liveroom.service.util

import android.content.Context
import android.os.Handler
import android.os.HandlerThread

class ToneThread(name: String, context: Context) : HandlerThread(name) {
    private var mHandler: Handler? = null
    private var engageCallRinger: EngageCallRinger = EngageCallRinger(context)
    private var isPlaying = false

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mHandler = Handler(looper) {
            if (it.what == 1) {
                if (isPlaying) {
                    return@Handler true
                }
                engageCallRinger.start()
                isPlaying = true
            } else if (it.what == 2) {
                engageCallRinger.stop()
                mHandler?.removeCallbacksAndMessages(null)
                isPlaying = false
            }
            true
        }
    }

    fun startBusyTone() {
        mHandler?.sendEmptyMessage(1)
    }

    fun stopBusyTone() {
        mHandler?.sendEmptyMessage(2)
    }
}
