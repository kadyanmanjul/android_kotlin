package com.joshtalks.joshskills.ui.voip

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.joshtalks.joshskills.core.CALL_RINGTONE_NOT_MUTE
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.ui.voip.util.WebRtcAudioManager
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.ExecutorService

abstract class BaseWebRtcService : Service() { /*,SensorEventListener*/
    protected val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")

    /*   private var proximityWakelock: PowerManager.WakeLock? = null
       private var cpuWakelock: PowerManager.WakeLock? = null
       private var isProximityNear = false
    */
    protected var joshAudioManager: WebRtcAudioManager? = null
    private var ringtonePlayer: MediaPlayer? = null
    private var ringingPlay = false
    private var vibrator: Vibrator? = null
    protected var compositeDisposable = CompositeDisposable()
    protected var mNotificationManager: NotificationManager? = null

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        executor.execute {
            try {
                joshAudioManager = WebRtcAudioManager(this)
               /* cpuWakelock = (getSystemService(POWER_SERVICE) as PowerManager?)?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "joshtalsk"
                )
                cpuWakelock?.acquire(60 * 1000L *//*1 minutes*//*)*/
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /*@SuppressLint("InvalidWakeLockTag")
    protected fun addSensor() {
        removeWakeLock()
        val sm: SensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val proximity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        try {
            proximityWakelock = (getSystemService(POWER_SERVICE) as PowerManager?)?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "joshtalsk-prx"
            )
            sm.registerListener(
                this@BaseWebRtcService,
                proximity,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun removeWakeLock() {
        try {
            if (proximityWakelock != null && proximityWakelock!!.isHeld) {
                proximityWakelock!!.release(RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            }
            if (cpuWakelock != null && cpuWakelock!!.isHeld) {
                cpuWakelock!!.release(RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    protected fun removeSensor() {
        try {
            removeWakeLock()
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            if (proximity != null) {
                sm.unregisterListener(this)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        try {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                if (am.isSpeakerphoneOn && am.isBluetoothScoOn) {
                    return
                }
                val newIsNear: Boolean = event.values[0] < min(event.sensor.maximumRange, 3F)
                checkIsNear(newIsNear)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun checkIsNear(newIsNear: Boolean) {
        if (newIsNear != isProximityNear) {
            isProximityNear = newIsNear
            try {
                if (isProximityNear) {
                    proximityWakelock?.acquire(30 * 60L * 60)
                } else {
                    proximityWakelock?.release(1)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
*/
    @SuppressLint("MissingPermission")
    protected fun startRingtoneAndVibration() {
        if (PrefManager.getBoolValue(CALL_RINGTONE_NOT_MUTE).not()) {
            return
        }
        if (ringingPlay) {
            return
        }
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
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
                } catch (ex: IllegalStateException) {
                }
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
                ringtonePlayer?.setDataSource(this, Uri.parse(notificationUri))
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
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(100, 250, 500, 750, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    protected fun stopRing() {
        try {
            ringtonePlayer?.run {
                stop()
                release()
                ringtonePlayer = null
                ringingPlay = false
            }
            vibrator?.run {
                cancel()
                vibrator = null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    protected fun executeEvent(event: String) {
        executor.execute {
            AppAnalytics.create(event)
                .addUserDetails()
                .push()
        }
    }

 /*   private fun showIncomingCallScreen(
        data: HashMap<String, String?>,
        autoPickupCall: Boolean = false
    ) {
        val callActivityIntent =
            Intent(
                this, WebRtcActivity::class.java
            ).apply {
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(AUTO_PICKUP_CALL, autoPickupCall)
                putExtra(CALL_USER_OBJ, data)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(callActivityIntent)
    }

    private fun isAppVisible(): Boolean {
        return if (JoshApplication.isAppVisible || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true
        else
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && JoshApplication.isAppVisible.not()
    }*/
}
