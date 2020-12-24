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
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipRatingFragment
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.HashMap

const val AUTO_PICKUP_CALL = "auto_pickup_call"
const val CALL_USER_OBJ = "call_user_obj"
const val CALL_TYPE = "call_type"


class WebRtcActivity : BaseActivity() {

    private lateinit var binding: ActivityCallingBinding
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false
    private val compositeDisposable = CompositeDisposable()
    private val userDetailLiveData: MutableLiveData<HashMap<String, String>> = MutableLiveData()

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

        override fun onConnect(callId: String) {
            Timber.tag(TAG).e("onConnect")
            runOnUiThread {
                binding.callStatus.text = getText(R.string.practice)
                startCallTimer()
            }
        }

        override fun onDisconnect(callId: String?, channelName: String?) {
            Timber.tag(TAG).e("onDisconnect")
            onStopCall()
            checkAndShowRating(callId, channelName)
        }

        override fun onCallReject(callId: String?) {
            Timber.tag(TAG).e("onCallReject")
            onStopCall()
            checkAndShowRating(callId)
        }

        override fun onServerConnect() {
            super.onServerConnect()
            val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            setUserInfo(map?.get(RTC_CALLER_UID_KEY))
        }
    }

    private fun checkAndShowRating(id: String?, channelName: String? = null) {
        Timber.tag(TAG).e("checkAndShowRating   %s %s", id, mBoundService?.getTimeOfTalk())
        if (mBoundService!!.getTimeOfTalk() > 0) {
            val prev = supportFragmentManager.findFragmentByTag(VoipRatingFragment::class.java.name)
            if (prev != null) {
                return
            }
            VoipRatingFragment.newInstance(channelName, mBoundService!!.getTimeOfTalk())
                .show(supportFragmentManager, VoipRatingFragment::class.java.name)
            return
        }
        this@WebRtcActivity.finishAndRemoveTask()
    }

    fun onStopCall() {
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
        removeRatingDialog()
        addObserver()
    }

    private fun addObserver() {
        userDetailLiveData.observe(this, Observer {
            binding.topic.text = it["topic_name"]
            binding.userDetail.text =
                it["name"]?.plus(" \n")?.plus(it["locality"])
            setImageInIV(it["profile_pic"])
        })

    }

    override fun onNewIntent(nIntent: Intent) {
        super.onNewIntent(nIntent)
        removeRatingDialog()
        try {
            val nMap = nIntent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            val nChannel = nMap?.get(RTC_CHANNEL_KEY)

            val oMap = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            val oChannel = oMap?.get(RTC_CHANNEL_KEY)

            if (nChannel != oChannel) {
                finish()
                startActivity(nIntent)
                overridePendingTransition(0, 0)
                return
            }
            this.intent = nIntent
            initCall()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun removeRatingDialog() {
        try {
            val prev = supportFragmentManager.findFragmentByTag(VoipRatingFragment::class.java.name)
            if (prev != null) {
                val df = prev as DialogFragment
                df.dismiss()
                supportFragmentManager.beginTransaction().run {
                    remove(prev)
                    addToBackStack(null)
                }
                supportFragmentManager.executePendingTransactions()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun initCall() {
        removeRatingDialog()
        val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        callType?.run {
            //val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            //setUserInfo(map?.get(RTC_CALLER_UID_KEY))
            if (CallType.OUTGOING == this) {
                binding.callStatus.text = getText(R.string.practice)
                startCallTimer()
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
            } else {
                val autoPickUp = intent.getBooleanExtra(AUTO_PICKUP_CALL, false)
                if (autoPickUp) {
                    acceptCall()
                    callDisViewEnable()
                    startCallTimer()
                } else {
                    binding.groupForIncoming.visibility = View.VISIBLE
                }
            }
            phoneConnectedStatus()
        }
    }

    private fun phoneConnectedStatus() {
        try {
            if (WebRtcService.isCallWasOnGoing) {
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
                binding.callTime.base =
                    SystemClock.elapsedRealtime() - (mBoundService?.getTimeOfTalk() ?: 0)
                binding.callTime.start()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun setUserInfo(uuid: String?) {
        if (uuid.isNullOrEmpty())
            return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = AppObjectController.p2pNetworkService.getUserDetailOnCall(uuid)
                userDetailLiveData.postValue(response)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
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
                    getName(),
                    ContextCompat.getColor(this, R.color.red)
                )
            binding.cImage.background = image
        } else {
            binding.cImage.setImage(imageUrl)
        }
    }

    private fun getName(): String {
        return try {
            binding.userDetail.text?.toString()?.substring(0, 2) ?: "US"
        } catch (ex: Exception) {
            "US"
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

    fun onDeclineCall() {
        WebRtcService.rejectCall()
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


    fun onDisconnectCall() {
        WebRtcService.disconnectCall()
    }

    private fun answerCall() {
        val data = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
        mBoundService?.answerCall(data)
        binding.callStatus.text = getText(R.string.practice)
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
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

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()
        binding.callTime.stop()
        unbindService(myConnection)
    }


    override fun onDestroy() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        super.onDestroy()
    }

    override fun onBackPressed() {
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(WebrtcEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onStopCall()
                    checkAndShowRating(null)
                }, {
                    it.printStackTrace()
                })
        )
    }
}