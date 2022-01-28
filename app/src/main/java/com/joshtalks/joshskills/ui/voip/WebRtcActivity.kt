package com.joshtalks.joshskills.ui.voip

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.cancelCallieDisconnectTimer
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isCallOnGoing
import com.joshtalks.joshskills.ui.voip.analytics.CurrentCallDetails
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics.Event.DISCONNECT
import com.joshtalks.joshskills.ui.voip.analytics.VoipEvent
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipCallFeedbackActivity
import com.joshtalks.joshskills.util.DateUtils
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

const val AUTO_PICKUP_CALL = "auto_pickup_call"
const val CALL_ACCEPT = "web_rtc_call_accept"
const val HIDE_INCOMING_UI = "hide_incoming_call_timer"
const val CALL_USER_OBJ = "call_user_obj"
const val DISCONNECT_REASON = "call_disconnect_reason"
const val CALL_TYPE = "call_type"
const val IS_DEMO_P2P = "is_demo_p2p"
const val IS_CALL_CONNECTED = "is_call_connected"
const val OPPOSITE_USER_UID = "opp_user_uid"

class WebRtcActivity : AppCompatActivity(), SensorEventListener {
    private val TAG = "WebRtcActivity"
    private lateinit var powerManager: PowerManager
    private lateinit var lock: PowerManager.WakeLock
    private lateinit var binding: ActivityCallingBinding
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false
    private lateinit var sensorManager: SensorManager
    private var proximity: Sensor? = null
    private lateinit var scope: CoroutineScope
    private val compositeDisposable = CompositeDisposable()
    private val userDetailLiveData: MutableLiveData<HashMap<String, String>> = MutableLiveData()
    private val viewModel: WebrtcViewModel by lazy {
        ViewModelProvider(this).get(WebrtcViewModel::class.java)
    }
    private var isAnimationCancled = false
    private var callType: CallType? = null
    private var callieId:String=""
    private var callerId:String=""

    val progressAnimator by lazy<ValueAnimator> {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            addUpdateListener {
                binding.incomingProgress.progress = ((animatedValue as Float) * 100).toInt()
            }
        }
    }

    val textAnimator by lazy<ValueAnimator> {
        ValueAnimator.ofFloat(0.8f, 1.2f, 1f).apply {
            duration = 300
            interpolator = BounceInterpolator()
            addUpdateListener {
                binding.incomingTimerTv.scaleX = it.animatedValue as Float
                binding.incomingTimerTv.scaleY = it.animatedValue as Float
            }
        }
    }


    companion object {
        var isIncomingCallHasNewChannel = false
        var isTimerCanceled = false

        fun startOutgoingCallActivity(
            activity: Activity,
            mapForOutgoing: HashMap<String, String?>,
            callType: CallType = CallType.OUTGOING,
            conversationId: String? = null,
            isDemoClass: Boolean = false
        ) {
            Intent(activity, WebRtcActivity::class.java).apply {
                putExtra(CALL_USER_OBJ, mapForOutgoing)
                putExtra(CALL_TYPE, callType)
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(IS_DEMO_P2P, isDemoClass)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivityForResult(this, 9999)
            }
        }


        fun getFavMissedCallbackIntent(partnerUid: Int, activity: Activity): Intent {
            val data = HashMap<String, String?>().apply {
                put(RTC_IS_FAVORITE, "true")
            }

            return Intent(activity, WebRtcActivity::class.java).apply {
                putExtra(RTC_PARTNER_ID, partnerUid)
                putExtra(CALL_TYPE, CallType.FAVORITE_MISSED_CALL)
                putExtra(CALL_USER_OBJ, data)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
            AppObjectController.uiHandler.postDelayed(
                {
                    updateStatusLabel()
                    startCallTimer()
                },
                500
            )
            PrefManager.put(P2P_LAST_CALL, true)
        }

        override fun onNewIncomingCallChannel() {
            super.onNewIncomingCallChannel()
            isIncomingCallHasNewChannel = true
        }

        override fun onIncomingCallConnected() {
            super.onIncomingCallConnected()
            if (!isIncomingCallHasNewChannel) {
                Log.d(TAG, "onIncomingCallConnected: stopAnimation")
                stopAnimation(true)
            }
        }

        override fun onIncomingCallUserConnected() {
            super.onIncomingCallUserConnected()
            if (isIncomingCallHasNewChannel) {
                Log.d(TAG, "onIncomingCallUserConnected: stopAnimation")
                setUserInfo(mBoundService?.getOppositeCallerId()?.toString(), isFromApi = true)
                stopAnimation(true)
            }
        }

        override fun onDisconnect(callId: String?, channelName: String?, time: Long) {
            Timber.tag(TAG).e("onDisconnect")
            onStopCall()
            checkAndShowRating(callId, channelName, time)
        }

        override fun onCallReject(callId: String?) {
            Timber.tag(TAG).e("onCallReject")
            onStopCall()
            checkAndShowRating(callId)
        }

        override fun onNetworkLost() {
            super.onNetworkLost()
            Timber.tag(TAG).e("onNetworkLost")
            AppObjectController.uiHandler.postDelayed(
                {
                    binding.connectionLost.text = getString(R.string.reconnecting)
                    binding.connectionLost.visibility = View.VISIBLE
                    binding.callTime.visibility = View.INVISIBLE
                    val state = CurrentCallDetails.state()
                    VoipAnalytics.push(
                        VoipAnalytics.Event.RECONNECTING,
                        agoraMentorUid = state.callieUid,
                        agoraCallId = state.callId,
                        timeStamp = DateUtils.getCurrentTimeStamp()
                    )
                },
                250
            )
        }

        override fun onNetworkReconnect() {
            super.onNetworkReconnect()
            Timber.tag(TAG).e("onNetworkReconnect")
            val isCallerJoin = mBoundService?.isCallerJoined ?: false
            if (isCallerJoin.not()) {
                return
            }

            AppObjectController.uiHandler.postDelayed(
                {
                    // binding.connectionLost.text = EMPTY
                    binding.connectionLost.visibility = View.INVISIBLE
                    binding.callTime.visibility = View.VISIBLE
                    val state = CurrentCallDetails.state()
                    VoipAnalytics.push(
                        VoipAnalytics.Event.CALL_RESTORED,
                        agoraMentorUid = state.callieUid,
                        agoraCallId = state.callId,
                        timeStamp = DateUtils.getCurrentTimeStamp()
                    )
                },
                250
            )
        }

        override fun onHoldCall() {
            super.onHoldCall()
            Timber.tag(TAG).e("onHoldCall")
            runOnUiThread {
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    VoipAnalytics.Event.ON_HOLD,
                    agoraMentorUid = state.callieUid,
                    agoraCallId = state.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
                CurrentCallDetails.callOnHold()
                binding.connectionLost.text = getString(R.string.hold_call)
                binding.connectionLost.visibility = View.VISIBLE
                binding.callTime.visibility = View.INVISIBLE
                binding.btnMute.isEnabled = false
            }
        }

        override fun onUnHoldCall() {
            super.onUnHoldCall()
            Timber.tag(TAG).e("onUnHoldCall")
            runOnUiThread {
                //      binding.connectionLost.text = EMPTY
                if (binding.connectionLost.text != getString(R.string.ringing)) {
                    val state = CurrentCallDetails.state()
                    if (state.isOnHold) {
                        VoipAnalytics.push(
                            VoipAnalytics.Event.RESUME,
                            agoraMentorUid = state.callieUid,
                            agoraCallId = state.callId,
                            timeStamp = DateUtils.getCurrentTimeStamp()
                        )
                        CurrentCallDetails.callResumed()
                    }
                    binding.connectionLost.visibility = View.INVISIBLE
                    binding.callTime.visibility = View.VISIBLE
                    binding.btnMute.isEnabled = true
                }
            }
        }

        override fun onSpeakerOff() {
            super.onSpeakerOff()
            AppObjectController.uiHandler.post {
                updateStatusLabel(binding.btnSpeaker, enable = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        lock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,"simplewakelock:wakelocktag")
        isIncomingCallHasNewChannel = false
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_calling)
        binding.lifecycleOwner = this
        binding.handler = this
        // setCallerInfoOnAppCreate()
        intent.printAllIntent()
        addObserver()
        AppAnalytics.create(AnalyticsEvent.OPEN_CALL_SCREEN_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        if (isCallOnGoing.value == false) {
            callMissedCallUser()
        }
    }

    private fun setCallerInfoOnAppCreate() {
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        map?.let {
            if (it.containsKey(RTC_CALLER_UID_KEY)) {
                setUserInfo(it[RTC_CALLER_UID_KEY])
            }
        }
    }

    private fun setCallScreenBackground() {
        if (isCallFavoritePP()) {
            binding.container.setBackgroundResource(R.drawable.voip_bg)
            return
        }
        binding.container.setBackgroundColor(Color.parseColor("#0D5CB8"))
    }

    private fun isCallFavoritePP(): Boolean {
        val isSetAsFavourite = mBoundService?.isFavorite()
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        val isFavouriteIntent = map != null && map.containsKey(RTC_IS_FAVORITE)
        if (isSetAsFavourite == false && isFavouriteIntent) {
            mBoundService?.setAsFavourite()
        }
        return (isSetAsFavourite == true || isFavouriteIntent)
    }

    private fun isCallGroupPP(): Boolean {
        val isSetAsGroup = mBoundService?.isGroupCall()
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        val isGroupCallIntent = map != null && map.containsKey(RTC_IS_GROUP_CALL)
        if (isSetAsGroup == false && isGroupCallIntent) {
            mBoundService?.setAsGroupCall()
        }
        return (isSetAsGroup == true || isGroupCallIntent)
    }

    private fun isNewUserCall(): Boolean {
        val isSetAsNewUserCall = mBoundService?.isNewUserCall()
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        val isNewUserIntent = map != null && map.containsKey(RTC_IS_NEW_USER_CALL)
        if (isSetAsNewUserCall == false && isNewUserIntent) {
            mBoundService?.setAsNewUserCall()
        }
        return (isSetAsNewUserCall == true || isNewUserIntent)
    }

    private fun callMissedCallUser() {
        val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        if (callType == null) {
            this@WebRtcActivity.finishAndRemoveTask()
        }
        callType?.run {
            if (CallType.FAVORITE_MISSED_CALL == this) {
                val pId = intent.getIntExtra(RTC_PARTNER_ID, -1)
                if (pId == -1) {
                    this@WebRtcActivity.finishAndRemoveTask()
                }
                updateStatusLabel()
                binding.groupForOutgoing.visibility = View.VISIBLE
                binding.connectionLost.text = getString(R.string.ringing)
                binding.connectionLost.visibility = View.VISIBLE
                binding.callTime.visibility = View.INVISIBLE
                viewModel.initMissedCall(pId.toString(), ::callback)
                setUserInfo(pId.toString())
                (getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager?)?.cancel(pId.hashCode())
            }
        }
    }

    // TODO: TIMEOUT DISCONNECT
    private fun addTimeOutUserDidNotPickCall() {
        Log.d(TAG, "addTimeOutUserDidNotPickCall: 346")
        AppObjectController.uiHandler.postDelayed(
            {
                onDisconnectCall(DISCONNECT.NO_USER_FOUND_FAILURE)
            },
            20000
        )
    }

    private fun callback(token: String, channelName: String, uid: Int) {
        val data: HashMap<String, String?> = HashMap()
        data.apply {
            put(RTC_TOKEN_KEY, token)
            put(RTC_CHANNEL_KEY, channelName)
            put(RTC_UID_KEY, uid.toString())
        }
        WebRtcService.startOutgoingCall(data)
        addTimeOutUserDidNotPickCall()
    }

    override fun onNewIntent(nIntent: Intent) {
        Log.d(TAG, "onNewIntent: 367")
        super.onNewIntent(nIntent)
        Log.d(TAG, "onNewIntent: ")
        try {
            val nMap = nIntent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            val nChannel = nMap?.get(RTC_CHANNEL_KEY)

            val oMap = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            val oChannel = oMap?.get(RTC_CHANNEL_KEY)
            Log.d(TAG, "onNewIntent(375): $nChannel .... $oChannel")
            if (nChannel != oChannel) {
                finish()
                startActivity(nIntent)
                overridePendingTransition(0, 0)
                return
            }
            if (nIntent.hasExtra(CALL_USER_OBJ) && nIntent.getSerializableExtra(CALL_USER_OBJ) != null) {
                this.intent = nIntent
            }
            initCall()
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        proximity?.also { proximity ->
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if(lock.isHeld) lock.release()
        compositeDisposable.clear()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        while (WebRtcService.incomingWaitJobsList.isNotEmpty())
            WebRtcService.incomingWaitJobsList.pop().cancel()
        if (binding.incomingTimerContainer.visibility == View.VISIBLE) {
            isAnimationCancled = true
            runOnUiThread {
                progressAnimator.cancel()
            }
            WebRtcService.disconnectCall(DISCONNECT.BACK_BUTTON_FAILURE)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
        binding.callTime.stop()
        unbindService(myConnection)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        super.onDestroy()
    }

    private fun addObserver() {
        userDetailLiveData.observe(
            this,
            {
                val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
                val isCallFromGroup = map != null && map.get(RTC_IS_GROUP_CALL) == "true"
                binding.topicHeader.visibility = View.VISIBLE
                binding.topicName.text = it["topic_name"]
                Log.d(TAG, "updateStatusLabel: addObserver -#@- ${map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME)}")
                if(isCallFromGroup || map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME).isNullOrBlank().not()) {
                    binding.tvGroupName.visibility = View.VISIBLE
                    binding.tvGroupName.text =
                        "from group \"${WebRtcService.currentCallingGroupName}\""
                }
                binding.callerName.text = it["name"]
                setImageInIV(it["profile_pic"])
                mBoundService?.setOppositeUserInfo(it)
            }
        )

        WebRtcService.isCallOnGoing.observe(this, { isCallOngoing ->
            if (isCallOngoing && !isIncomingCallHasNewChannel) {
                var partnerUid: String? = intent.getIntExtra(RTC_PARTNER_ID, -1).toString()
                if (partnerUid == "-1") {
                    val map =
                        intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
                    partnerUid = if (map?.get(RTC_CALLER_UID_KEY) != null) {
                        map[RTC_CALLER_UID_KEY]
                    } else {
                        mBoundService?.getOppositeCallerId()?.toString()
                    }
                }
                setUserInfo(partnerUid)
            }
        })

    }

    private fun initCall() {
        setCallScreenBackground()
        updateButtonStatus()
        callType = intent.getSerializableExtra(CALL_TYPE) as CallType?

        if (isCallFavoritePP() || WebRtcService.isCallOnGoing.value == true) {
            updateCallInfo()
        } /*else if (callType == CallType.INCOMING && WebRtcService.isCallWasOnGoing.value == true) {
            updateCallInfo()
        }*/

        callType?.run {
            updateStatusLabel()
            if (CallType.OUTGOING == this) {
                startCallTimer()
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
                val state = CurrentCallDetails.state()
                Log.d(TAG, "initCall: ---> CALL_CONNECT_SCREEN_VISUAL")
                VoipAnalytics.push(
                    VoipAnalytics.Event.CALL_CONNECT_SCREEN_VISUAL,
                    agoraMentorUid = state.callieUid,
                    agoraCallId = state.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
                CurrentCallDetails.callConnectedScreenVisible()
            } else if (CallType.INCOMING == this) {
                val autoPickUp = intent.getBooleanExtra(AUTO_PICKUP_CALL, false)
                val callAcceptApi = intent.getBooleanExtra(CALL_ACCEPT, true)
                if (autoPickUp) {
                    if(isCallOnGoing.value!=true) {
                        acceptCall(callAcceptApi)
                    }
                    if (isCallFavoritePP()) {
                        callDisViewEnable()
                        startCallTimer()
                    }
                } else {
                    binding.groupForIncoming.visibility = View.VISIBLE
                }
            }
            phoneConnectedStatus()
        }
        if (intent.hasExtra(IS_CALL_CONNECTED)) {
            startCallTimer()
        }
    }

    private fun updateCallInfo() {
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        if (map?.get(RTC_CALLER_UID_KEY) != null) {
            setUserInfo(map[RTC_CALLER_UID_KEY])
        } else {
            setUserInfo(mBoundService?.getOppositeCallerId()?.toString())
        }
    }

    private fun phoneConnectedStatus() {
        try {
            if (WebRtcService.isCallOnGoing.value == true) {
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
                startCallTimer()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun updateStatusLabel() {
        lifecycleScope.launchWhenCreated {
            binding.tvGroupName.visibility = View.GONE
            val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            val isCallFromGroup = map != null && map.get(RTC_IS_GROUP_CALL) == "true"
            Log.d(TAG, "updateStatusLabel: ${map} : $isCallFromGroup")
            val callConnected = mBoundService?.isCallerJoined ?: false
            val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
            Log.d(TAG, "updateStatusLabel: ${map} callType ${callType}  isCallFavoritePP():${isCallFavoritePP()}  callConnected:${callConnected} isCallFromGroup:${isCallFromGroup}")
            callerId= map?.get("caller_uid").toString()
            callieId =CurrentCallDetails.callieUid
            callType?.run {
                if (CallType.FAVORITE_MISSED_CALL == this || CallType.OUTGOING == this) {
                    if (callConnected && isCallFavoritePP()) {
                        binding.callStatus.text = getText(R.string.pp_connected)
                        return@run
                    } else if (callConnected.not() && isCallFavoritePP()) {
                        binding.callStatus.text = getText(R.string.pp_calling)
                        return@run
                    }
                } else {
                    if (callConnected && isCallFavoritePP()) {
                        binding.callStatus.text = getText(R.string.pp_connected)
                        return@run
                    } else if (callConnected.not() && isCallFavoritePP()) {
                        binding.callStatus.text = getText(R.string.pp_favorite_incoming)
                        return@run
                    } else if (callConnected.not() && isCallGroupPP()) {
                        binding.callStatus.text = getText(R.string.pp_group_incoming)
                        binding.callerName.text = "${map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME)}"
                        setImageInIV(map?.get(RTC_WEB_GROUP_PHOTO))
                        return@run
                    } else if (callConnected.not() && isCallFavoritePP().not()) {
                        binding.callStatus.text = "Incoming Call from"
                        binding.callerName.text = "Practice Partner"
                        return@run
                    } else if (callConnected && isCallFavoritePP().not()) {
                        binding.callStatus.text = "Practice with Partner"
                        Log.d(TAG, "updateStatusLabel:  P2P -#- ${map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME)}")
                        if(isCallFromGroup || map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME).isNullOrBlank().not()) {
                            binding.tvGroupName.visibility = View.VISIBLE
                            binding.tvGroupName.text =
                                "from group \"${WebRtcService.currentCallingGroupName}\""
                        }
                        return@run
                    }
                }
                binding.callStatus.text = "Practice with Partner"
                if(isCallFromGroup || map?.get(RTC_WEB_GROUP_CALL_GROUP_NAME).isNullOrBlank().not()) {
                    binding.tvGroupName.visibility = View.VISIBLE
                    binding.tvGroupName.text =
                        "from group \"${WebRtcService.currentCallingGroupName}\""
                }
            }
        }
    }

    private fun updateButtonStatus() {
        mBoundService?.getSpeaker()?.let {
            updateStatusLabel(binding.btnSpeaker, it.not())
        }
        mBoundService?.getMic()?.let {
            if (it.not()) {
                updateStatusLabel(binding.btnMute, false)
            }
        }
    }

    private fun getCallTime(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(
            (mBoundService?.getTimeOfTalk() ?: 1).toLong()
        ) * 1000
    }

    private fun setUserInfo(uuid: String?, isFromApi: Boolean = false) {
        if (uuid.isNullOrEmpty())
            return
        if (!isFromApi && userDetailLiveData.value != null) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            delay(750)
            try {
                val userInfo = mBoundService?.getOppositeUserInfo()
                val userDetails =
                    if (!isFromApi && userInfo != null && userInfo["uid"] == uuid)
                        userInfo
                    else
                        AppObjectController.p2pNetworkService.getUserDetailOnCall(uuid)
                userDetailLiveData.postValue(userDetails)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun setImageInIV(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            val image = TextDrawable.builder()
                .beginConfig()
                .height(binding.cImage.height)
                .width(binding.cImage.width)
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
            binding.callerName.text.toString().substring(0, 2)
        } catch (ex: Exception) {
            "US"
        }
    }

    private fun startCallTimer() {
        binding.callTime.base = SystemClock.elapsedRealtime() - getCallTime()
        binding.callTime.start()
        Log.d(TAG, "startCallTimer: ")
        if (WebRtcService.pstnCallState == CallState.CALL_STATE_IDLE) {
            if (binding.connectionLost.text == getString(R.string.reconnecting)) {
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    VoipAnalytics.Event.CALL_RESTORED,
                    agoraMentorUid = state.callieUid,
                    agoraCallId = state.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
            }
            binding.connectionLost.visibility = View.INVISIBLE
            binding.callTime.visibility = View.VISIBLE
        } else {
            binding.connectionLost.visibility = View.VISIBLE
            binding.callTime.visibility = View.INVISIBLE
        }
    }

    private fun callDisViewEnable() {
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
    }

    fun switchAudioMode() {
        updateStatusLabel(binding.btnSpeaker, mBoundService!!.getSpeaker())
        mBoundService?.switchAudioSpeaker()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    fun switchTalkMode() {
        updateStatusLabel(binding.btnMute, mBoundService!!.getMic().not())
        mBoundService?.switchSpeck()
    }

    fun onDeclineCall() {
        WebRtcService.rejectCall()
    }

    fun acceptCall(callAcceptApi: Boolean = true, isUserPickUp: Boolean = false) {
        Timber.d("acceptCall: ")
        val courseExpiryTime = PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS)
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT).not() &&
            courseExpiryTime != 0L &&
            courseExpiryTime < System.currentTimeMillis()
        ) {
            startActivity(
                FreeTrialPaymentActivity.getFreeTrialPaymentActivityIntent(
                    this,
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID),
                    courseExpiryTime,
                    true
                )
            )
        } else if (!isTimerCanceled) {
            if (isUserPickUp) {
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    VoipAnalytics.Event.CALL_ACCEPT,
                    agoraMentorUid = state.callieUid,
                    agoraCallId = state.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
            } /*else {
                Log.d(TAG, "acceptCall: ---> CALL_CONNECT_SCREEN_VISUAL")
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    VoipAnalytics.Event.CALL_CONNECT_SCREEN_VISUAL,
                    agoraMentorUid = state.callieUid,
                    agoraCallId = state.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
                CurrentCallDetails.callConnectedScreenVisible()
            }*/
            cancelCallieDisconnectTimer()
            if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                acceptCallForDemo(callAcceptApi)
            } else {
                acceptCallForNormal(callAcceptApi)
            }
        }
    }

    private fun acceptCallForDemo(callAcceptApi: Boolean = true) {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            answerCall(callAcceptApi)
            return
        }

        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            answerCall(callAcceptApi)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            onDisconnectCall(DISCONNECT.LOCATION_PERMISSION_FAILURE)
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
            }
        )
    }

    private fun acceptCallForNormal(callAcceptApi: Boolean = true) {
        if (PermissionUtils.isCallingPermissionEnabled(AppObjectController.joshApplication)) {
            answerCall(callAcceptApi)
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            answerCall(callAcceptApi)
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
            }
        )
    }

    fun onDisconnectCall(reason: VoipEvent) {
        WebRtcService.disconnectCall(reason)
        AppObjectController.uiHandler.postDelayed(
            {
                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            },
            1000
        )
    }

    private fun answerCall(callAcceptApi: Boolean = true) {
        val data = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        if (data == null) {
            this.finishAndRemoveTask()
            return
        }
        if (!isAnimationCancled)
            mBoundService?.answerCall(data, callAcceptApi)
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
        val hideIncomingCallUi = intent.getBooleanExtra(HIDE_INCOMING_UI, false)
        if (!isCallFavoritePP() && !hideIncomingCallUi && !isAnimationCancled)
            startIncomingTimer()
        AppAnalytics.create(AnalyticsEvent.ANSWER_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun updateStatusLabel(view: AppCompatImageButton, enable: Boolean) {
        if (enable) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.dis_color_10f)
            view.imageTintList = ContextCompat.getColorStateList(applicationContext, R.color.white)
        } else {
            view.backgroundTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.white)
            view.imageTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.grey_61)
        }
    }

    private fun checkAndShowRating(id: String?, channelName: String? = null, callTime: Long = 0) {
        Timber.tag(TAG)
            .e("checkAndShowRating   %s %s %s", id, mBoundService?.getTimeOfTalk(), callTime)
        if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
            PrefManager.put(DEMO_P2P_CALLEE_NAME, userDetailLiveData.value?.get("name").toString())
        }
        showCallRatingScreen(callTime, channelName)
    }

    private fun showCallRatingScreen(callTime: Long, channelName: String?) {
        var time = mBoundService?.getTimeOfTalk() ?: 0
        if (time <= 0) {
            time = callTime
        }
        val channelName2 =
            if (channelName.isNullOrBlank().not()) channelName else mBoundService?.channelName
        if (time > 0 && channelName2.isNullOrEmpty().not()) {
            runOnUiThread {
                try {
                    binding.placeholderBg.visibility = View.VISIBLE
                    VoipCallFeedbackActivity.startPtoPFeedbackActivity(
                        channelName = channelName2,
                        callTime = time,
                        callerName = userDetailLiveData.value?.get("name"),
                        callerImage = userDetailLiveData.value?.get("profile_pic"),
                        yourName = if (User.getInstance().firstName.isNullOrBlank()) "New User" else User.getInstance().firstName,
                        yourAgoraId = mBoundService?.getUserAgoraId(),
                        activity = this,
                        flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                        callerId = Integer.parseInt(callerId),
                        currentUserId =Integer.parseInt(callieId)
                    )
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
                this.finish()
            }
            mBoundService?.setOppositeUserInfo(null)
            return
        }
        this@WebRtcActivity.finishAndRemoveTask()
    }

    fun onStopCall() {
        //     SoundPoolManager.getInstance(this).release()
        AppAnalytics.create(AnalyticsEvent.DISCONNECT_CALL_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(WebrtcEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        onStopCall()
                        checkAndShowRating(mBoundService?.getCallId(), mBoundService?.channelName)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        showSnackBar(binding.container, Snackbar.LENGTH_LONG, it.pointsSnackBarText)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            // SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            playSnackbarSound(this)
        }
    }

    private fun startIncomingTimer() {
        stopAnimation()
        isAnimationCancled = false
        binding.incomingTimerContainer.visibility = View.VISIBLE
        binding.cImage.visibility = View.INVISIBLE
        binding.topicName.visibility = View.INVISIBLE
        binding.topicHeader.visibility = View.INVISIBLE
        binding.callerName.visibility = View.INVISIBLE
        binding.callStatus.visibility = View.INVISIBLE
        setIncomingText()
        var counter = 35
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                if (counter != 0 && !isAnimationCancled) {
                    counter -= 1
                    binding.incomingTimerTv.text = "$counter"
                    textAnimator.start()
                    progressAnimator.start()
                } else {
                    if (!isIncomingCallHasNewChannel) {
                        val state = CurrentCallDetails.state()
                        VoipAnalytics.push(
                            VoipAnalytics.Event.RECEIVE_TIMER_STOP,
                            agoraMentorUid = state.callieUid,
                            agoraCallId = state.callId,
                            timeStamp = DateUtils.getCurrentTimeStamp()
                        )
                    }
                    if (counter <= 0) {
                        isIncomingCallHasNewChannel = false
                        WebRtcService.noUserFoundCallDisconnect()
                        finish()
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (textAnimator.isStarted && textAnimator.isRunning)
                    textAnimator.cancel()
            }

            override fun onAnimationRepeat(animation: Animator?) {}

        })
        val state = CurrentCallDetails.state()
        VoipAnalytics.push(
            VoipAnalytics.Event.RECEIVE_TIMER_START,
            agoraMentorUid = state.callieUid,
            agoraCallId = state.callId,
            timeStamp = DateUtils.getCurrentTimeStamp()
        )
        progressAnimator.start()
    }

    private fun setIncomingText() {
        binding.tvIncomingCallHeading.visibility = View.VISIBLE
        binding.tvIncomingCallSubHeading.visibility = View.VISIBLE
        binding.tvIncomingCallHeading.text = "Practice with Partner"
        binding.tvIncomingCallSubHeading.text = "Call is being connected..."
        binding.tvIncomingCallSubHeading.textSize = 15f
        binding.tvIncomingCallSubHeading.setTypeface(binding.callStatus.typeface, Typeface.NORMAL)
    }

    @Synchronized
    private fun stopAnimation(isCallConnected: Boolean = false) {
        Log.d(TAG, "stopAnimation: ")
        isAnimationCancled = true
        runOnUiThread {
            progressAnimator.cancel()
            binding.incomingTimerContainer.visibility = View.INVISIBLE
            binding.tvIncomingCallHeading.visibility = View.INVISIBLE
            binding.tvIncomingCallSubHeading.visibility = View.INVISIBLE
            binding.groupForOutgoing.visibility = View.VISIBLE
            binding.cImage.visibility = View.VISIBLE
            binding.topicName.visibility = View.VISIBLE
            binding.topicHeader.visibility = View.VISIBLE
            binding.callerName.visibility = View.VISIBLE
            binding.callStatus.visibility = View.VISIBLE
            if (isCallConnected) {
                Log.d(TAG, "stopAnimation: ---> CALL_CONNECT_SCREEN_VISUAL")
                VoipAnalytics.push(
                    VoipAnalytics.Event.CALL_CONNECT_SCREEN_VISUAL,
                    agoraMentorUid = CurrentCallDetails.callieUid,
                    agoraCallId = CurrentCallDetails.callId,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
                CurrentCallDetails.callConnectedScreenVisible()
            }
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {

        if (p0?.values?.get(0)?.compareTo(0.0) == 0) {
//            face is near to sensor
            if (mBoundService?.getSpeaker() == false) {
                turnScreenOff()
            }
        } else {
//            face is away from sensor
            turnScreenOn()
        }
    }

    private fun turnScreenOff() {
        if (!lock.isHeld) lock.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun turnScreenOn() {
        if (lock.isHeld) lock.release()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}
