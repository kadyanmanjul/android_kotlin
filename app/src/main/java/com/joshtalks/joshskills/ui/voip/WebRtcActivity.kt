package com.joshtalks.joshskills.ui.voip

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
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
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.databinding.AudioDeviceBottomsheetBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.cancelCallieDisconnectTimer
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isCallOnGoing
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipCallFeedbackActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


const val AUTO_PICKUP_CALL = "auto_pickup_call"
const val CALL_ACCEPT = "web_rtc_call_accept"
const val HIDE_INCOMING_UI = "hide_incoming_call_timer"
const val CALL_USER_OBJ = "call_user_obj"
const val CALL_TYPE = "call_type"
const val IS_DEMO_P2P = "is_demo_p2p"
const val IS_CALL_CONNECTED = "is_call_connected"
const val OPPOSITE_USER_UID = "opp_user_uid"
const val BLUETOOTH_SOC_STREAM = 6

class WebRtcActivity : AppCompatActivity() {
    private val TAG = "WebRtcActivity"
    private lateinit var binding: ActivityCallingBinding
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false
    private lateinit var scope: CoroutineScope
    private val compositeDisposable = CompositeDisposable()
    private val userDetailLiveData: MutableLiveData<HashMap<String, String>> = MutableLiveData()
    private val viewModel: WebrtcViewModel by lazy {
        ViewModelProvider(this).get(WebrtcViewModel::class.java)
    }
    private var isAnimationCancled = false
    private var callType: CallType? = null
    private val am by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

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
            Log.d(TAG, "startOutgoingCallActivity: ${mapForOutgoing}")
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
            WebRtcService.currentButtonState = VoipButtonState.NONE
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
                stopAnimation()
            }
        }

        override fun onIncomingCallUserConnected() {
            super.onIncomingCallUserConnected()
            if (isIncomingCallHasNewChannel) {
                Log.d(TAG, "onIncomingCallUserConnected: stopAnimation")
                setUserInfo(mBoundService?.getOppositeCallerId()?.toString(), isFromApi = true)
                stopAnimation()
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
                    VoipAnalytics.push(VoipAnalytics.Event.RECONNECTING)
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
                },
                250
            )
        }

        override fun onHoldCall() {
            super.onHoldCall()
            Timber.tag(TAG).e("onHoldCall")
            runOnUiThread {
                binding.connectionLost.text = getString(R.string.hold_call)
                binding.connectionLost.visibility = View.VISIBLE
                binding.callTime.visibility = View.INVISIBLE
                binding.btnMute.isEnabled = false
            }
        }

        /*override fun switchChannel(data: HashMap<String, String?>) {
            super.switchChannel(data)

            val callActivityIntent =
                Intent(this@WebRtcActivity, WebRtcActivity::class.java).apply {
                    putExtra(CALL_TYPE, CallType.INCOMING)
                    putExtra(AUTO_PICKUP_CALL, true)
                    putExtra(HIDE_INCOMING_UI, true)
                    putExtra(CALL_USER_OBJ, data)
                    if (isCallFavoritePP()) {
                        putExtra(RTC_IS_FAVORITE, "true")
                    }
                }
            Log.d(TAG, "switchChannel: ")
            finish()
            startActivity(callActivityIntent)
            overridePendingTransition(0, 0)

        }*/

        override fun onUnHoldCall() {
            super.onUnHoldCall()
            Timber.tag(TAG).e("onUnHoldCall")
            runOnUiThread {
                //      binding.connectionLost.text = EMPTY
                if (binding.connectionLost.text != getString(R.string.ringing)) {
                    binding.connectionLost.visibility = View.INVISIBLE
                    binding.callTime.visibility = View.VISIBLE
                    binding.btnMute.isEnabled = true
                }
            }
        }

        override fun onBluetoothStateChanged(isOn: Boolean) {
            super.onBluetoothStateChanged(isOn)
            Timber.tag("BLUETOOTH").d("onBluetoothStateChanged --- $isOn")
            /*AppObjectController.uiHandler.post {
                updateStatusLabel(binding.btnBluetooth, enable = !isOn)
            }*/
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        //viewModel.isWiredHeadphoneConnected.set(isWiredHeadSetOn)
        WebRtcService.BLUETOOTH_RETRY_COUNT = 0
        WebRtcService.HANDSET_RETRY_COUNT = 0
        WebRtcService.currentButtonState = VoipButtonState.NONE
        binding = DataBindingUtil.setContentView(this, R.layout.activity_calling)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.vm = viewModel
        binding.executePendingBindings()
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

    /*  private fun isCallFavoritePP(): Boolean {
          return false
      }*/

    private fun callMissedCallUser() {
        val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        if (callType == null) {
            Log.d(TAG, "callMissedCallUser: --- 2")
            this@WebRtcActivity.finishAndRemoveTask()
        }
        callType?.run {
            if (CallType.FAVORITE_MISSED_CALL == this) {
                val pId = intent.getIntExtra(RTC_PARTNER_ID, -1)
                if (pId == -1) {
                    Log.d(TAG, "callMissedCallUser: finishAndRemoveTask -- 1")
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

    private fun addTimeOutUserDidNotPickCall() {
        AppObjectController.uiHandler.postDelayed(
            {
                onDisconnectCall()
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
        super.onNewIntent(nIntent)
        Log.d(TAG, "onNewIntent: ")
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
        initAudioStateListener()
    }

    private fun initAudioStateListener() {
        scope = CoroutineScope(Job() + Dispatchers.Default)
        scope.launch {
            VoipAudioState.audioState.collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        State.BluetoothOn -> {
                            Log.d(TAG, "initAudioStateListener: BluetoothOn")
                            viewModel.audioState.set(CallAudioState.BLUETOOTH)
                            Glide.with(this@WebRtcActivity)
                                .load(R.drawable.ic_bluetooth)
                                .into(binding.btnAudioList)
                        }
                        State.SpeakerOn -> {
                            Log.d(TAG, "initAudioStateListener: SpeakerOn")
                            viewModel.audioState.set(CallAudioState.SPEAKER)
                            Glide.with(this@WebRtcActivity)
                                .load(R.drawable.ic_speaker_icon)
                                .into(binding.btnAudioList)
                        }
                        is State.Default -> {
                            Log.d(
                                TAG,
                                "initAudioStateListener: DEFAULT ${it.isWiredHeadphonePluggedIn}"
                            )
                            if (it.isWiredHeadphonePluggedIn) {
                                Log.d(TAG, "initAudioStateListener: Setting HEADPHONE")
                                viewModel.audioState.set(CallAudioState.HEADPHONE)
                                viewModel.isWiredHeadphoneConnected.set(true)
                                Glide.with(this@WebRtcActivity)
                                    .load(R.drawable.ic_headphone_with_mic)
                                    .into(binding.btnAudioList)
                            } else {
                                Log.d(TAG, "initAudioStateListener: Setting HEADSET")
                                viewModel.audioState.set(CallAudioState.HANDSET)
                                viewModel.isWiredHeadphoneConnected.set(false)
                                Glide.with(this@WebRtcActivity)
                                    .load(R.drawable.ic_handset)
                                    .into(binding.btnAudioList)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeAudioStateListener() = scope.cancel()

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        while (WebRtcService.incomingWaitJobsList.isNotEmpty())
            WebRtcService.incomingWaitJobsList.pop().cancel()
        if (binding.incomingTimerContainer.visibility == View.VISIBLE) {
            isAnimationCancled = true
            runOnUiThread {
                progressAnimator.cancel()
            }
            WebRtcService.disconnectCall()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.callTime.stop()
        removeAudioStateListener()
        Log.d(TAG, "onStop: ")
        Log.d(TAG, "onStop: is Finishing --> $isFinishing")
        Log.d(TAG, "onStop: isCallOnGoing --> ${isCallOnGoing.value}")
        val hideIncomingCallUi = intent.getBooleanExtra(HIDE_INCOMING_UI, false)
        /*if (callType == CallType.INCOMING && !isAnimationCancled && isCallOnGoing.value == false) {
            isAnimationCancled = true
            runOnUiThread {
                progressAnimator.cancel()
            }
            WebRtcService.disconnectCall()
        }*/
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
                Log.d(TAG, "addObserver: $it")
                binding.topicHeader.visibility = View.VISIBLE
                binding.topicName.text = it["topic_name"]
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
        //updateButtonStatus()
        callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        //showToast("Call Type --> $callType")
        if (isCallFavoritePP() || WebRtcService.isCallOnGoing.value == true) {
            updateCallInfo()
        } /*else if (callType == CallType.INCOMING && WebRtcService.isCallWasOnGoing.value == true) {
            updateCallInfo()
        }*/

        callType?.run {
            Log.d(TAG, "initCall: $this")
            updateStatusLabel()
            if (CallType.OUTGOING == this) {
                startCallTimer()
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
            } else if (CallType.INCOMING == this) {
                Log.d(TAG, "initCall: OUT")
                val autoPickUp = intent.getBooleanExtra(AUTO_PICKUP_CALL, false)
                val callAcceptApi = intent.getBooleanExtra(CALL_ACCEPT, true)
                if (autoPickUp) {
                    Log.d(TAG, "initCall: autoPickUp --> $autoPickUp")
                    acceptCall(callAcceptApi)
                    if (isCallFavoritePP()) {
                        callDisViewEnable()
                        startCallTimer()
                    }
                } else {
                    Log.d(TAG, "initCall: ELSE")
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
            val callConnected = mBoundService?.isCallerJoined ?: false
            val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
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
                    } else if (callConnected.not() && isCallFavoritePP().not()) {
                        binding.callStatus.text = "Incoming Call from"
                        binding.callerName.text = "Practice Partner"
                        return@run
                    } else if (callConnected && isCallFavoritePP().not()) {
                        binding.callStatus.text = "Practice with Partner"
                        return@run
                    }
                }
                binding.callStatus.text = "Practice with Partner "
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
        if (WebRtcService.pstnCallState == CallState.CALL_STATE_IDLE) {
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

    private fun turnOnBluetooth() {
        mBoundService?.turnOnBluetooth(VoipButtonState.BLUETOOTH)
        //volumeControlStream = BLUETOOTH_SOC_STREAM
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    private fun turnOnDefault() {
        mBoundService?.turnOnDefault(VoipButtonState.DEFAULT)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    private fun turnOnSpeaker() {
        mBoundService?.turnOnSpeaker(VoipButtonState.SPEAKER)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    fun switchTalkMode() {
        updateStatusLabel(binding.btnMute, mBoundService!!.getMic().not())
        mBoundService?.switchSpeck()
    }

    fun onDeclineCall() {
        VoipAnalytics.push(VoipAnalytics.Event.CALL_DECLINED)
        WebRtcService.rejectCall()
    }

    fun acceptCall(callAcceptApi: Boolean = true, isUserPickUp: Boolean = false) {
        if (!isTimerCanceled) {
            if (isUserPickUp)
                VoipAnalytics.push(VoipAnalytics.Event.CALL_ACCEPT)
            cancelCallieDisconnectTimer()
            if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                acceptCallForDemo(callAcceptApi)
            } else {
                acceptCallForNormal(callAcceptApi)
            }
        }
    }

    private fun acceptCallForDemo(callAcceptApi: Boolean = true) {
        if (PermissionUtils.isDemoCallingPermissionEnabled(this)) {
            Log.d(TAG, "acceptCallForDemo: ")
            answerCall(callAcceptApi)
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            Log.d(TAG, "onPermissionsChecked: acceptCallForDemo")
                            answerCall(callAcceptApi)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            onDisconnectCall()
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
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            Log.d(TAG, "acceptCallForNormal: ")
            answerCall(callAcceptApi)
            return
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            Log.d(TAG, "onPermissionsChecked: acceptCallForNormal")
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

    fun onDisconnectCall() {
        WebRtcService.disconnectCall()
        AppObjectController.uiHandler.postDelayed(
            {
                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            },
            1000
        )
    }

    private fun answerCall(callAcceptApi: Boolean = true) {
        val data = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        Log.d(TAG, "answerCall: $data")
        if (data == null) {
            Log.d(TAG, "answerCall: finishAndRemoveTask -- 3")
            this.finishAndRemoveTask()
            return
        }
        if (!isAnimationCancled)
            mBoundService?.answerCall(data, callAcceptApi)
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
        //TODO: OutGoing Screen Visual for OutGoing and Incoming
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

    private fun enableButton(view: AppCompatImageButton) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(applicationContext, R.color.white)
        view.imageTintList =
            ContextCompat.getColorStateList(applicationContext, R.color.grey_61)
    }

    private fun disableButton(view: AppCompatImageButton) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(applicationContext, R.color.dis_color_10f)
        view.imageTintList = ContextCompat.getColorStateList(applicationContext, R.color.white)
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
                binding.placeholderBg.visibility = View.VISIBLE
                VoipCallFeedbackActivity.startPtoPFeedbackActivity(
                    channelName = channelName2,
                    callTime = time,
                    callerName = userDetailLiveData.value?.get("name"),
                    callerImage = userDetailLiveData.value?.get("profile_pic"),
                    yourName = if (User.getInstance().firstName.isNullOrBlank()) "New User" else User.getInstance().firstName,
                    yourAgoraId = mBoundService?.getUserAgoraId(),
                    activity = this,
                    flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
                this.finish()
            }
            mBoundService?.setOppositeUserInfo(null)
            return
        }
        Log.d(TAG, "showCallRatingScreen: finishAndRemoveTask --- 4")
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

    fun openAudioDeviceBottomSheet() {
        val audioDialogSheet = BottomSheetDialog(this)
        val isWiredHeadsetOn = am.isWiredHeadsetOn
        viewModel.isWiredHeadphoneConnected.set(isWiredHeadsetOn)
        viewModel.isBluetoothHeadsetConnected.set(isBluetoothHeadsetConnected())
        val audioDeviceBottomsheetBinding: AudioDeviceBottomsheetBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.audio_device_bottomsheet, null, false
        )
        audioDeviceBottomsheetBinding.vm = viewModel
        audioDeviceBottomsheetBinding.audioBluetooth.setOnClickListener {
            turnOnBluetooth()
            audioDialogSheet.dismiss()
        }

        audioDeviceBottomsheetBinding.audioSpeaker.setOnClickListener {
            turnOnSpeaker()
            audioDialogSheet.dismiss()
        }

        audioDeviceBottomsheetBinding.audioDefault.setOnClickListener {
            turnOnDefault()
            audioDialogSheet.dismiss()
        }

        audioDialogSheet.setContentView(audioDeviceBottomsheetBinding.root)
        audioDialogSheet.setCancelable(true)
        audioDialogSheet.setCanceledOnTouchOutside(true)
        audioDialogSheet.show()
    }

    private fun startIncomingTimer() {
        Log.d(TAG, "startIncomingTimer: stopAnimation")
        stopAnimation()
        isAnimationCancled = false
        binding.incomingTimerContainer.visibility = View.VISIBLE
        binding.cImage.visibility = View.INVISIBLE
        binding.topicName.visibility = View.INVISIBLE
        binding.topicHeader.visibility = View.INVISIBLE
        binding.callerName.visibility = View.INVISIBLE
        binding.callStatus.visibility = View.INVISIBLE
        setIncomingText()
        var counter = 25
        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                if (counter != 0 && !isAnimationCancled) {
                    counter -= 1
                    binding.incomingTimerTv.text = "$counter"
                    textAnimator.start()
                    progressAnimator.start()
                } else {
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
    private fun stopAnimation() {
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
        }
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (bluetoothAdapter != null && bluetoothAdapter?.isEnabled == true
                && bluetoothAdapter?.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)
    }
}
