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

object BeepTimer {

//    private val soundPool by lazy {
//        SoundPool.Builder()
//            .build()
//    }

    private var soundPool: SoundPool? = null

    private var beepJob: Job? = null

    fun startBeepSound(context: Context) {
        if (PrefManager.getBeepTimerStatus()) {

            val soundPool = SoundPool.Builder().build()

            val beepSound = soundPool.load(context, R.raw.beep, 1)

            var timer = 0L

            soundPool.setOnLoadCompleteListener(object : SoundPool.OnLoadCompleteListener {
                override fun onLoadComplete(p0: SoundPool?, p1: Int, p2: Int) {
                    Log.d("experttimer", "p1: $p1")
                    Log.d("experttimer", "p2: $p2")
                    beepJob = CoroutineScope(Dispatchers.IO).launch {
                        Log.d("experttimer", "shall play beep timer => ${timer < TIMER_DURATION}")
                        delay(1000)
                        while (timer < TIMER_DURATION) {
                            Log.d("experttimer", "playing beep timer")
                            p0!!.play(beepSound, 1f, 1f, 1, 0, 1.0f)
                            timer += 2000
                            delay(BEEP_INTERVAL)
                        }
                    }
                }

            })
        }
    }

    fun stopBeepSound() {
        beepJob?.cancel()
        soundPool?.release()
    }

    private const val BEEP_INTERVAL = 2000L
    const val TIMER_DURATION = 30000L

}