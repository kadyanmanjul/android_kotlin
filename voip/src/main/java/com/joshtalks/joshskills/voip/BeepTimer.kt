package com.joshtalks.joshskills.voip

import android.content.Context
import android.media.SoundPool
import android.util.Log
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.*

/**
This class is responsible to start beep timer when user has only 15 seconds left to disconnect the call.
Because his wallet amount is about to finish in 15 seconds.
 */

class BeepTimer(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val soundPool by lazy {
        SoundPool.Builder()
            .build()
    }

    private val beepSound by lazy {
        soundPool.load(context, R.raw.beep, 1)
    }

    fun startBeepSound() {
        if (PrefManager.getBeepTimerStatus()) {
            var timer = 0L
            Log.d("experttimer", "started beep timer")

//        soundPool.setOnLoadCompleteListener { soundPool, i, i2 ->
            coroutineScope.launch {
                Log.d("experttimer", "shall play beep timer => ${timer < TIMER_DURATION}")
                while (timer < TIMER_DURATION) {
                    Log.d("experttimer", "playing beep timer")
                    soundPool.play(beepSound, 1f, 1f, 1, 0, 1.0f)
                    timer += 2000
                    delay(BEEP_INTERVAL)
                }
            }
        }

//        }
    }

    fun stopBeepSound() {
        coroutineScope.cancel()
        soundPool.release()
    }

    companion object {
        private const val BEEP_INTERVAL = 2000L
        const val TIMER_DURATION = 30000L
    }

}