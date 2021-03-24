package com.joshtalks.joshskills.ui.voip

import android.app.Activity
import android.app.NotificationManager
import android.app.Service
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
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.DEMO_P2P_CALLEE_NAME
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.core.printAllIntent
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityCallingBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipCallFeedbackView
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
import kotlinx.coroutines.launch
import timber.log.Timber

const val AUTO_PICKUP_CALL = "auto_pickup_call"
const val CALL_USER_OBJ = "call_user_obj"
const val CALL_TYPE = "call_type"
const val IS_DEMO_P2P = "is_demo_p2p"

class WebRtcActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallingBinding
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false
    private val compositeDisposable = CompositeDisposable()
    private val userDetailLiveData: MutableLiveData<HashMap<String, String>> = MutableLiveData()
    private val viewModel: WebrtcViewModel by lazy {
        ViewModelProvider(this).get(WebrtcViewModel::class.java)
    }

    companion object {
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
            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
            AppObjectController.uiHandler.postDelayed(
                {
                    updateStatusLabel()
                    startCallTimer()
                    binding.connectionLost.visibility = View.GONE
                },
                500
            )
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

        override fun onServerConnect() {
            super.onServerConnect()
            Timber.tag(TAG).e("onServerConnect")
            val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
            setUserInfo(map?.get(RTC_CALLER_UID_KEY))
        }

        override fun onNetworkLost() {
            super.onNetworkLost()
            Timber.tag(TAG).e("onNetworkLost")
            AppObjectController.uiHandler.postDelayed(
                {
                    binding.connectionLost.text = getString(R.string.reconnecting)
                    binding.connectionLost.visibility = View.VISIBLE
                    binding.callTime.visibility = View.INVISIBLE
                },
                250
            )
        }

        override fun onNetworkReconnect() {
            super.onNetworkReconnect()
            Timber.tag(TAG).e("onNetworkReconnect")
            val isCallerJoin = mBoundService?.isCallerJoin ?: false
            if (isCallerJoin.not()) {
                return
            }

            AppObjectController.uiHandler.postDelayed(
                {
                    // binding.connectionLost.text = EMPTY
                    binding.connectionLost.visibility = View.GONE
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
            }
        }

        override fun onUnHoldCall() {
            super.onUnHoldCall()
            Timber.tag(TAG).e("onUnHoldCall")
            runOnUiThread {
                //      binding.connectionLost.text = EMPTY
                binding.connectionLost.visibility = View.GONE
                binding.callTime.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        setCallerInfoOnAppCreate()
        intent.printAllIntent()
        addObserver()
        AppAnalytics.create(AnalyticsEvent.OPEN_CALL_SCREEN_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        callMissedCallUser()
    }

    private fun setCallerInfoOnAppCreate() {
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        map?.let {
            if (it.containsKey(RTC_CALLER_UID_KEY)) {
                setUserInfo(it[RTC_CALLER_UID_KEY])
            }
        }
    }

    private fun setFavoriteUIScreen() {
        if (isCallFavoritePP()) {
            binding.container.setBackgroundResource(R.drawable.voip_bg)
            return
        }
        binding.container.setBackgroundColor(Color.parseColor("#0D5CB8"))
    }

    private fun isCallFavoritePP(): Boolean {
        val f = mBoundService?.isFavorite()
        val map = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        val c = map != null && map.containsKey(RTC_IS_FAVORITE)
        if (c) {
            return c
        }
        return f ?: c
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
                setUserInfo(pId.toString())
                viewModel.initMissedCall(pId.toString(), ::callback)
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
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        binding.callTime.stop()
        unbindService(myConnection)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        super.onDestroy()
    }

    override fun onBackPressed() {
    }

    private fun addObserver() {
        userDetailLiveData.observe(
            this,
            {
                binding.topicName.text = it["topic_name"]
                binding.callerName.text = it["name"]
                setImageInIV(it["profile_pic"])
            }
        )
    }

    private fun initCall() {
        setFavoriteUIScreen()
        val callType = intent.getSerializableExtra(CALL_TYPE) as CallType?
        callType?.run {
            updateStatusLabel()
            if (CallType.OUTGOING == this) {
                startCallTimer()
                binding.groupForIncoming.visibility = View.GONE
                binding.groupForOutgoing.visibility = View.VISIBLE
            } else if (CallType.INCOMING == this) {
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
                binding.callTime.base = SystemClock.elapsedRealtime() - getCallTime()
                binding.callTime.start()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun updateStatusLabel() {
        val callConnected = mBoundService?.isCallerJoin ?: false
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
                }
            }
            binding.callStatus.text = getText(R.string.practice)
        }
    }

    private fun removeRatingDialog() {
        try {
            val prev =
                supportFragmentManager.findFragmentByTag(VoipCallFeedbackView::class.java.name)
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

    private fun getCallTime(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(
            (mBoundService?.getTimeOfTalk() ?: 1).toLong()
        ) * 1000
    }

    private fun setUserInfo(uuid: String?) {
        if (uuid.isNullOrEmpty())
            return
        if (userDetailLiveData.value != null) {
            return
        }
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
        if (WebRtcService.phoneCallState == CallState.CALL_STATE_IDLE) {
            binding.callTime.visibility = View.VISIBLE
        } else {
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

    fun acceptCall() {
        if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
            acceptCallForDemo()
        } else {
            acceptCallForNormal()
        }
    }

    private fun acceptCallForDemo() {
        if (PermissionUtils.isDemoCallingPermissionEnabled(this)) {
            answerCall()
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            answerCall()
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

    private fun acceptCallForNormal() {
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

    private fun answerCall() {
        val data = intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
        if (data == null) {
            this.finishAndRemoveTask()
            return
        }
        mBoundService?.answerCall(data)
        binding.groupForIncoming.visibility = View.GONE
        binding.groupForOutgoing.visibility = View.VISIBLE
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
        showCallRatingScreen(callTime)
    }

    private fun showCallRatingScreen(callTime: Long) {
        var time = mBoundService?.getTimeOfTalk() ?: 0
        if (time <= 0) {
            time = callTime
        }
        val channelName = mBoundService?.channelName
        if (time > 0 && channelName.isNullOrEmpty().not()) {
            runOnUiThread {
                binding.placeholderBg.visibility = View.VISIBLE
            }
            VoipCallFeedbackView.showCallRatingDialog(
                supportFragmentManager,
                channelName = channelName,
                callTime = time,
                callerName = userDetailLiveData.value?.get("name"),
                callerImage = userDetailLiveData.value?.get("profile_pic"),
                yourName = if (User.getInstance().firstName.isNullOrBlank()) "New User" else User.getInstance().firstName,
                yourAgoraId = mBoundService?.getUserAgoraId()
            )
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
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        val prev =
                            supportFragmentManager.findFragmentByTag(VoipCallFeedbackView::class.java.name)
                        if (prev != null) {
                            return@subscribe
                        }
                        onStopCall()
                        checkAndShowRating(mBoundService?.getCallId())
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
}
