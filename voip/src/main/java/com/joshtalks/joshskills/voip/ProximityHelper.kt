package com.joshtalks.joshskills.voip

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

class ProximityHelper private  constructor(val application: Application) {
    val STOPED = 0
    val STARTED = 1
    private val TAG = "ProximityHelper"
    private var sensorManager : SensorManager? = null
    private var proximity: Sensor? = null
    private var accelerometer: Sensor? = null
    private var powerManager: PowerManager? = null
    private var wakeLock : PowerManager.WakeLock? = null
    private var state = STOPED
    private val scope = CoroutineScope(newSingleThreadContext("ProximityHelper") + CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.d("ProximityHelper", "CoroutineExceptionHandler : $throwable")
        throwable.printStackTrace()
    })
    private val proximityCallback = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if(event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                if(event.values[0] >= event.sensor.maximumRange)
                    turnScreenOn()
                else
                    turnScreenOff()
            }
        }

        override fun onAccuracyChanged(event: Sensor?, p1: Int) {


        }
    }

    companion object {
        var INSTANCE : ProximityHelper? = null

        fun getInstance(application: Application) : ProximityHelper? {
            if(INSTANCE == null) {
                INSTANCE = ProximityHelper(application)
                INSTANCE?.initialiseProximityHelper()
            }
            return INSTANCE
        }
    }

    private fun initialiseProximityHelper() {
        try {
            sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: throw IllegalArgumentException("Has no sensor")
            powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "simplewakelock:wakelocktag")
        } catch (e : Exception) {

        }
    }

    private fun reset() {
        sensorManager = null
        proximity = null
        powerManager = null
        wakeLock = null
    }

    private fun turnScreenOn() {
        if (wakeLock?.isHeld == true)
            wakeLock?.release()
    }

    private fun turnScreenOff() {
        if (wakeLock?.isHeld == false)
            wakeLock?.acquire()
    }

    fun start() {
        if(PrefManager.isProximitySensorOn()) {
            Log.d(TAG, "start: isProximitySensorOn = true")
            scope.launch {
                if (state == STOPED) {
                    proximity?.also { proximity ->
                        sensorManager?.registerListener(
                            proximityCallback,
                            proximity,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    }
                    state = STARTED
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            if(state == STARTED) {
                turnScreenOn()
                sensorManager?.unregisterListener(proximityCallback)
                reset()
                INSTANCE = null
                state = STOPED
            }
        }
    }

}