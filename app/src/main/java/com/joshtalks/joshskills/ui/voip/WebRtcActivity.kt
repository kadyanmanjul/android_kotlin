package com.joshtalks.joshskills.ui.voip

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.ui.voip.util.AudioPlayer
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.HashMap

const val CALL_USER_ID = "call_user_id"
const val IS_INCOMING_CALL = "is_incoming_call"
const val INCOMING_CALL_JSON_OBJECT = "incoming_json_call_object"
const val AUTO_PICKUP_CALL = "auto_pickup_call"
const val CALL_USER_OBJ = "call_user_obj"
const val CALL_TYPE = "call_type"
const val INCOMING_CALL_USER_OBJ = "incoming_call_user_obj"


class WebRtcActivity : BaseActivity() {

    private lateinit var binding: ActivityCallingBinding
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false

    companion object {

        fun startOutgoingCallActivity(
            activity: Activity,
            voipCallDetailModel: VoipCallDetailModel? = null
        ) {
            Intent(activity, WebRtcActivity::class.java).apply {
                putExtra(CALL_USER_ID, voipCallDetailModel)
                putExtra(CALL_USER_OBJ, getMapForOutgoing())
                putExtra(CALL_TYPE, CallType.OUTGOING)
            }.run {
                activity.startActivityForResult(this, 9999)
            }
        }

        private fun getMapForOutgoing(
            destId: String = "cc0f6cc567934b579b555d49904768542198822111217",
            topic: String = "aise hi",
            name: String = "Sunil",
            location: String = "Japan",
        ): HashMap<String, String> {
            return object : HashMap<String, String>() {
                init {
                    put("X-PH-Destination", destId)
                    put("X-PH-TOPIC", topic)
                    put("X-PH-NAME", name)
                    put("X-PH-LOCATION", location)
                }
            }
        }

    }

    inner class ReceivedCallConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            initCall()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_calling)
        binding.lifecycleOwner = this
        binding.handler = this
        AppAnalytics.create(AnalyticsEvent.OPEN_CALL_SCREEN_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun initCall() {
        val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        callType?.run {
            val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String>
            setUserInfo(map)
            if (CallType.OUTGOING == this) {
                WebRtcService.startOutgoingCall(map)
                startCallTimer()
                callDisViewEnable()
            } else {
                val autoPickUp = intent.getBooleanExtra(AUTO_PICKUP_CALL, false)
                if (autoPickUp) {
                    mBoundService?.answerCall()
                    startCallTimer()
                    callDisViewEnable()
                } else {
                    binding.groupForIncoming.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setUserInfo(map: HashMap<String, String>) {
        binding.callStatus.text = map["X-PH-TOPIC"]
        binding.userDetail.text = map["X-PH-NAME"] + " \n" + map["X-PH-LOCATION"]
    }

    private fun startCallTimer() {
        binding.callTime.base = SystemClock.elapsedRealtime()
        binding.callTime.start()
    }

    private fun callDisViewEnable() {
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
    }

    fun switchAudioMode() {
        mBoundService?.switchAudioSpeaker()
        updateStatus(binding.btnSpeaker, mBoundService!!.getSpeaker())
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

    }

    fun switchTalkMode() {
        mBoundService?.switchSpeck()
        updateStatus(binding.btnMute, mBoundService!!.getMic().not())
    }

    fun acceptCall() {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            answerCall()
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            answerCall()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                this@WebRtcActivity,
                                message = R.string.call_permission_permanent_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    fun onDeclineCall() {
        mBoundService?.rejectCall()
        onBackPressed()
    }

    fun onDisconnectCall() {
        mBoundService?.endCall()
        onBackPressed()
    }

    private fun answerCall() {
        AudioPlayer.getInstance().stopProgressTone()
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
        mBoundService?.answerCall()
        startCallTimer()
        AppAnalytics.create(AnalyticsEvent.ANSWER_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun updateStatus(view: View, enable: Boolean) {
        if (enable) {
            view.alpha = 1.0F
            view.backgroundTintList =
                ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.green)
        } else {
            view.alpha = 0.5F
            view.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.transparent
            )
        }
    }

    private fun stopCall() {
        try {
            AudioPlayer.getInstance().stopProgressTone()
            SoundPoolManager.getInstance(this).release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, WebRtcService::class.java),
            ReceivedCallConnection(),
            BIND_AUTO_CREATE
        )
    }

    override fun onBackPressed() {
        stopCall()
        AppAnalytics.create(AnalyticsEvent.DISCONNECT_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        val resultIntent = Intent()
        setResult(RESULT_OK, resultIntent)
        finishAndRemoveTask()
    }
}