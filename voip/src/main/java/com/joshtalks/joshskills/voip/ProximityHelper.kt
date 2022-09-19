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
import kotlin.math.acos
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ProximityHelper private constructor(val application: Application) {
    val STOPED = 0
    val STARTED = 1
    private val TAG = "ProximityHelper"
    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var accelerometer: Sensor? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var state = STOPED
    var shouldGetInclination = true
    private val scope =
        CoroutineScope(newSingleThreadContext("ProximityHelper") + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.d("ProximityHelper", "CoroutineExceptionHandler : $throwable")
            throwable.printStackTrace()
        })
    private val proximityCallback = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                if (event.values[0] >= event.sensor.maximumRange)
                    turnScreenOn()
                else
                    turnScreenOff()
            }
        }

        override fun onAccuracyChanged(event: Sensor?, p1: Int) {


        }
    }
    private val accelerometerCallback = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                event.values?.let {
                    // TODO: Need to lock the variable
                    if (shouldGetInclination) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (shouldGetInclination) {
                                shouldGetInclination = false
                                val angle = it.getInclination()
                                if(angle < 25 || angle > 155)
                                    turnScreenOn()
                                delay(1000)
                                shouldGetInclination = true
                            }
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(event: Sensor?, p1: Int) {


        }
    }

    private suspend fun FloatArray.getInclination() : Int {
        Log.d(TAG, "onSensorChanged: X = ${this[0]}")
        Log.d(TAG, "onSensorChanged: Y = ${this[1]}")
        Log.d(TAG, "onSensorChanged: Z = ${this[2]}")
        val normOfData = sqrt(this[0] * this[0] + this[1] * this[1] + this[2] * this[2])
        val normZ = this[2] / normOfData

        return Math.toDegrees(acos(normZ).toDouble()).roundToInt()
    }

    companion object {
        var INSTANCE: ProximityHelper? = null

        fun getInstance(application: Application): ProximityHelper? {
            if (INSTANCE == null) {
                INSTANCE = ProximityHelper(application)
                INSTANCE?.initialiseProximityHelper()
            }
            return INSTANCE
        }
    }

    private fun initialiseProximityHelper() {
        try {
            sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
                ?: throw IllegalArgumentException("Has no sensor")
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                ?: throw IllegalArgumentException("Has no sensor")
            powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "simplewakelock:wakelocktag"
            )
        } catch (e: Exception) {

        }
    }

    private fun reset() {
        sensorManager = null
        proximity = null
        accelerometer = null
        powerManager = null
        wakeLock = null
    }

    private fun turnScreenOn() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            sensorManager?.unregisterListener(accelerometerCallback)
        }
    }

    private fun turnScreenOff() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            accelerometer?.also { proximity ->
                sensorManager?.registerListener(
                    accelerometerCallback,
                    proximity,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    fun start() {
        if (PrefManager.isProximitySensorOn()) {
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
            if (state == STARTED) {
                turnScreenOn()
                sensorManager?.unregisterListener(proximityCallback)
                reset()
                INSTANCE = null
                state = STOPED
            }
        }
    }

}