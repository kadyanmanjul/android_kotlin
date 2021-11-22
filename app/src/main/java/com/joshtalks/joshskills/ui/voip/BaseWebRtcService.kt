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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseWebRtcService : Service() { /*,SensorEventListener*/
    protected val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")

    protected var joshAudioManager: WebRtcAudioManager? = null
    private var ringtonePlayer: MediaPlayer? = null
    private var ringingPlay = false
    private var vibrator: Vibrator? = null
    protected var compositeDisposable = CompositeDisposable()
    protected var mNotificationManager: NotificationManager? = null
    val jobs = arrayListOf<Job>()

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        try {
            joshAudioManager = WebRtcAudioManager(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

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
        try {
            jobs += CoroutineScope(Dispatchers.IO).launch  {
                AppAnalytics.create(event)
                    .addUserDetails()
                    .push()
            }
        } catch (ex:Exception) { ex.printStackTrace() }
    }

}
