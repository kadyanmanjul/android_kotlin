package com.joshtalks.joshskills.ui.voip

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Color
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
import com.joshtalks.joshskills.core.CountUpTimer
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.printAll
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.ui.voip.util.AudioPlayer
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipRatingFragment
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.HashMap

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
    private var countUpTimer = CountUpTimer(false)

    companion object {
        fun startOutgoingCallActivity(
            activity: Activity,
            mapForOutgoing: HashMap<String, String?>
        ) {
            Intent(activity, WebRtcActivity::class.java).apply {
                putExtra(CALL_USER_OBJ, mapForOutgoing)
                putExtra(CALL_TYPE, CallType.OUTGOING)
            }.run {
                activity.startActivityForResult(this, 9999)
            }
        }
    }

    private var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callback)
            initCall()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    private var callback: WebRtcCallback = object : WebRtcCallback {
        override fun onRinging() {
            Timber.tag(TAG).e("onRinging")
        }

        override fun onConnect() {
            Timber.tag(TAG).e("onConnect")
            runOnUiThread {
                binding.callStatus.text = getText(R.string.practice)
                try {
                    countUpTimer.lap()
                    countUpTimer.resume()
                    startCallTimer()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        override fun onDisconnect(id: String?) {
            Timber.tag(TAG).e("onDisconnect")
            onStopCall()
            AppObjectController.uiHandler.post {
                countUpTimer.pause()
            }
            checkAndShowRating(id)
        }

        override fun onCallDisconnect(id: String?) {
            Timber.tag(TAG).e("onCallDisconnect")
            checkAndShowRating(id)
        }

        override fun onCallReject(id: String?) {
            Timber.tag(TAG).e("onCallReject")
            checkAndShowRating(id)
        }

        override fun onSelfDisconnect(id: String?) {
            Timber.tag(TAG).e("onSelfDisconnect")
            checkAndShowRating(id)
        }

        override fun onIncomingCallHangup(id: String?) {
            Timber.tag(TAG).e("onIncomingCallHangup")
            checkAndShowRating(id)
        }

        private fun checkAndShowRating(id: String?) {
            Timber.tag(TAG).e("checkAndShowRating   %s", id)
            if (id.isNullOrEmpty().not() && countUpTimer.time > 0) {
                VoipRatingFragment.newInstance(id, countUpTimer.time)
                    .show(supportFragmentManager, "voip_rating_dialog_fragment")
                return
            }
            this@WebRtcActivity.finish()
        }
    }

    fun onStopCall() {
        AudioPlayer.getInstance().stopProgressTone()
        SoundPoolManager.getInstance(this).release()
        AppAnalytics.create(AnalyticsEvent.DISCONNECT_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        requestWindowFeature(Window.FEATURE_NO_TITLE)
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
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

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
            val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
            map.printAll()
            if (CallType.INCOMING == this) {
                setUserInfoForIncomingCal(map.get("X-PH-MOBILEUUID"))
                val autoPickUp = intent.getBooleanExtra(AUTO_PICKUP_CALL, false)
                if (autoPickUp) {
                    acceptCall()
                    callDisViewEnable()
                    startCallTimer()
                } else {
                    binding.groupForIncoming.visibility = View.VISIBLE
                }
            } else {
                setUserInfoForOutgoing(map)
                setImageInIV(map["X-PH-IMAGE_URL"])
                binding.callStatus.text = getText(R.string.practice)
                countUpTimer.lap()
                countUpTimer.resume()
                startCallTimer()
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
            }
            phoneConnectedStatus(this)
        }
    }

    private fun phoneConnectedStatus(callType: CallType) {
        if (WebRtcService.isCallWasOnGoing) {
            binding.groupForIncoming.visibility = View.GONE
            binding.groupForOutgoing.visibility = View.VISIBLE
        }
    }

    private fun setUserInfoForIncomingCal(uuid: String?) {
        if (uuid.isNullOrEmpty())
            return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = AppObjectController.commonNetworkService.callMentorInfo(uuid)
                CoroutineScope(Dispatchers.Main).launch {
                    binding.topic.text = response["topic_name"]
                    binding.userDetail.text =
                        response["name"]?.plus(" \n")?.plus(response["locality"])
                    setImageInIV(response["profile_pic"])
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun setUserInfoForOutgoing(map: HashMap<String, String?>) {
        binding.topic.text = map["X-PH-TOPICNAME"]
        binding.userDetail.text = map["X-PH-CALLIENAME"]?.plus(" \n")?.plus(map["X-PH-LOCALITY"])
    }

    private fun setImageInIV(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            val image = TextDrawable.builder()
                .beginConfig()
                .textColor(Color.WHITE)
                .fontSize(Utils.dpToPx(24))
                .toUpperCase()
                .endConfig()
                .buildRound(
                    binding.userDetail.text?.toString()?.substring(0, 2) ?: "US",
                    ContextCompat.getColor(this, R.color.red)
                )
            binding.cImage.background = image
        } else {
            binding.cImage.setImage(imageUrl)
        }

    }

    private fun startCallTimer() {
        binding.callTime.visibility = View.VISIBLE
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
    }

    fun onDisconnectCall() {
        mBoundService?.endCall()
    }

    private fun answerCall() {
        binding.callStatus.text = getText(R.string.practice)
        AudioPlayer.getInstance().stopProgressTone()
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
        mBoundService?.answerCall()
        AppAnalytics.create(AnalyticsEvent.ANSWER_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun updateStatus(view: View, enable: Boolean) {
        if (enable) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.blue49)
        } else {
            view.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.transparent
            )
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, WebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(myConnection)
    }

    override fun onDestroy() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        super.onDestroy()
    }

    override fun onBackPressed() {
    }
}