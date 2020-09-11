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
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.ui.voip.util.AudioPlayer
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sinch.android.rtc.PushPair
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallDirection
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.CallState
import com.sinch.gson.JsonElement
import com.sinch.gson.JsonParser

const val CALL_USER_ID = "call_user_id"

const val IS_INCOMING_CALL = "is_incoming_call"
const val INCOMING_CALL_JSON_OBJECT = "incoming_json_call_object"

class WebRtcActivity : BaseActivity() {

    private lateinit var binding: ActivityCallingBinding
    private var call: Call? = null
    private var sinchClient: SinchClient? = null
    private var mBoundService: WebRtcService? = null
    private var voipCallDetailModel: VoipCallDetailModel? = null
    private var isSpeakerEnable = false
    private var isSpeckEnable = false
    private var mServiceBound = false
    private var roomId: String = ""

    companion object {
        fun startVoipActivity(activity: Activity, voipCallDetailModel: VoipCallDetailModel) {
            Intent(activity, WebRtcActivity::class.java).apply {
                putExtra(CALL_USER_ID, voipCallDetailModel)
            }.run {
                activity.startActivityForResult(this, 9999)
            }
        }
    }

    private var sinchCallListener = object : CallListener {
        override fun onCallEnded(endedCall: Call) {
            Log.e("onClientStarted", "ERROR" + endedCall.details?.error)
            volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
            SoundPoolManager.getInstance(applicationContext).playDisconnect()
            binding.callTime.stop()
            call = null
            binding.callTime.visibility = View.GONE
            binding.callStatus.text = "Call Ended"
            binding.callStatus.visibility = View.VISIBLE
            AppObjectController.uiHandler.postDelayed({
                onBackPressed()
            }, 800)
        }

        override fun onCallEstablished(establishedCall: Call) {
            if (CallDirection.OUTGOING == establishedCall.direction) {
                AudioPlayer.getInstance().stopProgressTone()
            }
            SoundPoolManager.getInstance(applicationContext).stopRinging()
            Log.e("onClientStarted", "onCallEstablished")
            binding.callStatus.text = "Connecting"
            AppObjectController.uiHandler.postDelayed({
                binding.callStatus.visibility = View.GONE
                binding.callTime.visibility = View.VISIBLE
                binding.callTime.base = SystemClock.elapsedRealtime()
                binding.callTime.start()
            }, 800)
        }

        override fun onCallProgressing(progressingCall: Call) {
            volumeControlStream = AudioManager.STREAM_VOICE_CALL
            if (CallDirection.OUTGOING == progressingCall.direction) {
                AudioPlayer.getInstance().playProgressTone()
            }
            binding.callStatus.text = "Ringing"
            Log.e("onClientStarted", "onCallProgressing")

            /* sinchClient?.audioController?.enableAutomaticAudioRouting(
                 true,
                 AudioController.UseSpeakerphone.SPEAKERPHONE_AUTO
             )*/
        }

        override fun onShouldSendPushNotification(call: Call, pushPairs: List<PushPair>) {
            Log.e("onClientStarted", "onShouldSendPushNotification")
        }
    }

    inner class ReceivedCallConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.call?.run {
                if (call == null) {
                    call = this
                    call?.addCallListener(sinchCallListener)
                }
            }
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
        initSinch()
        actionAfterSinchInit()
    }

    override fun onStart() {
        super.onStart()
        AppObjectController.joshApplication.bindService(
            Intent(AppObjectController.joshApplication, WebRtcService::class.java),
            ReceivedCallConnection(),
            BIND_AUTO_CREATE
        )
    }

    private fun initSinch() {
        sinchClient = AppObjectController.initSinchClient()
        sinchClient?.run {
            if (isStarted.not()) {
                startListeningOnActiveConnection()
                start()
            }
        }
    }

    private fun actionAfterSinchInit() {
        try {
            if (intent.hasExtra(CALL_USER_ID)) {//You are caller
                binding.groupForOutgoing.visibility = View.VISIBLE
                voipCallDetailModel = intent.getParcelableExtra(CALL_USER_ID)
                voipCallDetailModel?.run {
                    binding.userName.text = name
                    binding.userLocation.text = locality
                    binding.topicTextview.text = topic
                    initCall(mentorId)
                }
            } else {//You are receiver
                binding.groupForIncoming.visibility = View.VISIBLE
                if (intent.hasExtra(INCOMING_CALL_JSON_OBJECT)) {
                    SoundPoolManager.getInstance(applicationContext).playRinging()
                    val jsonElement: JsonElement =
                        JsonParser().parse(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                    jsonElement.asJsonObject.get("name")?.asString?.run {
                        binding.userName.text = this
                    }
                    jsonElement.asJsonObject.get("locality")?.asString?.run {
                        binding.userLocation.text = this
                    }
                    jsonElement.asJsonObject.get("topic")?.asString?.run {
                        binding.topicTextview.text = this
                    }

                    jsonElement.asJsonObject.get("mentor_id")?.asString?.run {
                        roomId = this
                        call = sinchClient?.callClient?.getCall(this)
                        call?.addCallListener(sinchCallListener)
                        WebRtcService.isCallWasOnGoing = true
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun initCall(mentorId: String) {
        try {
            val json =
                AppObjectController.gsonMapper.toJson(voipCallDetailModel?.getOutgoingCallObject())
            val map = hashMapOf<String, String?>()
            map["data"] = json
            call = sinchClient?.callClient?.callUser(mentorId, map)
            call?.addCallListener(sinchCallListener)
            sinchClient?.setPushNotificationDisplayName(json)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun switchAudioMode() {
        isSpeakerEnable = !isSpeakerEnable
        updateStatus(binding.btnSpeaker, isSpeakerEnable)
        if (isSpeakerEnable) {
            sinchClient?.audioController?.enableSpeaker()
        } else {
            sinchClient?.audioController?.disableSpeaker()
        }
    }

    fun switchTalkMode() {
        isSpeckEnable = !isSpeckEnable
        updateStatus(binding.btnMute, isSpeckEnable)
        if (isSpeckEnable) {
            sinchClient?.audioController?.mute()
        } else {
            sinchClient?.audioController?.unmute()
        }
    }

    fun actionOnCalling() {
        if (call == null) {
            onBackPressed()
            return
        }
        call?.let {
            if (CallDirection.OUTGOING == it.direction) {
                it.hangup()
                onBackPressed()
            } else {
                if (CallState.ESTABLISHED == it.state) {
                    it.hangup()
                    onBackPressed()
                } else {
                    answerCallPermissionCheck(it)
                }
            }
        }
    }

    private fun answerCallPermissionCheck(call: Call) {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            answerCall(call)
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            answerCall(call)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@WebRtcActivity)
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

    private fun answerCall(call: Call) {
        AudioPlayer.getInstance().stopProgressTone()
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
        call.answer()
        call.addCallListener(sinchCallListener)
    }


    private fun stopCall() {
        try {
            AudioPlayer.getInstance().stopProgressTone()
            call?.run {
                this.hangup()
            }
            SoundPoolManager.getInstance(this).release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onBackPressed() {
        stopCall()
        val resultIntent = Intent()
        setResult(RESULT_OK, resultIntent)
        finishAndRemoveTask()
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


}