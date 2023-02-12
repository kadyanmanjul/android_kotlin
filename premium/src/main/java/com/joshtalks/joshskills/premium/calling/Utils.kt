package com.joshtalks.joshskills.premium.calling

import android.app.Application
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.joshtalks.joshskills.PremiumApplication
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.constants.COURSE_ID
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.base.model.NotificationData
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.premium.calling.Utils.Companion.courseId
import com.joshtalks.joshskills.premium.calling.constant.Category
import com.joshtalks.joshskills.premium.calling.data.CallingRemoteService
import com.joshtalks.joshskills.premium.calling.recordinganalytics.CallRecordingAnalytics
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.utils.isBlocked
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// TODO: Must Refactor
val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)
private const val TAG = "Utils"
private const val UPLOAD_ANALYTICS_WORKER_NAME="Upload_Analytics_Api"
private var currentToast : Toast? = null

fun Long.inSeconds() : Long {
    return TimeUnit.MILLISECONDS.toSeconds(this)
}

fun Context.updateLastCallDetails(
    duration: Long,
    remoteUserName: String,
    remoteUserImage: String?,
    remotesUserMentorId : String,
    callId: Int,
    callType: Category,
    remoteUserAgoraId: Int,
    localUserAgoraId: Int,
    channelName: String,
    topicName: String,
) {
    Log.d(TAG, "updateStartCallTime: ")
//    val values = ContentValues(9).apply {
//        put(CALL_DURATION, duration)
//        put(REMOTE_USER_NAME, remoteUserName)
//        put(REMOTE_USER_IMAGE, remoteUserImage)
//        put(REMOTE_USER_AGORA_ID, remoteUserAgoraId)
//        put(CALL_ID, callId)
//        put(CALL_TYPE, callType.ordinal)
//        put(CHANNEL_NAME, channelName)
//        put(TOPIC_NAME, topicName)
//        put(CURRENT_USER_AGORA_ID, localUserAgoraId)
//        put(REMOTE_USER_MENTOR_ID, remotesUserMentorId)
//
//    }
//    val data = contentResolver.insert(
//        Uri.parse(CONTENT_URI + CALL_DISCONNECTED_URI),
//        values
//    )
    VoipPref.updateLastCallDetails(
        duration = duration,
        remoteUserImage = remoteUserImage,
        remoteUserName = remoteUserName,
        remoteUserAgoraId = remoteUserAgoraId,
        callId = callId,
        callType = callType.ordinal,
        localUserAgoraId = localUserAgoraId,
        channelName = channelName,
        topicName = topicName,
        showFpp = "true",
        remoteUserMentorId = remotesUserMentorId
    )
    Log.d(TAG, "updateStartCallTime: Data --> duration - $duration")
}

fun Context.updateStartTime(startTime : Long) {
    Log.d(TAG, "updateStartCallTime: $startTime")
    VoipPref.updateCurrentCallStartTime(startTime)
}

suspend fun showToast(msg: String,length:Int=Toast.LENGTH_SHORT) {
    withContext(Dispatchers.Main) {
        currentToast?.cancel()
        currentToast =  Toast.makeText(Utils.context, msg, length)
        currentToast?.show()
    }
}

fun Context.getApiHeader(): ApiHeader {
    try {
        val apiHeader = ApiHeader(
            token = "JWT " + PrefManager.getStringValue(API_TOKEN),
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toString(),
            userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
            acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
        )
        return apiHeader
    } catch (e : Exception) {
        return ApiHeader.empty()
    }
}

fun Context.getNotificationData(): NotificationData {
    try {
        val isBlockedOrFtEnded = if (PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false)) {
            when {
                isBlocked() -> true
                else -> PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
            }
        } else false

        val notificationData = NotificationData(
            title = getNotificationTitle(
                PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
                isBlockedOrFtEnded
            ),
            body = getNotificationBody(
                PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
                isBlockedOrFtEnded
            )
        )
        return notificationData
    } catch (e : Exception) {
        e.printStackTrace()
        return NotificationData.default()
    }
}

fun Context.getMentorId(): String {
    var mentorId = ""
    try {
        mentorId = Mentor.getInstance().getId()
    } catch (ex: Exception) {
        ex.printStackTrace()
        return ""
    }
    return mentorId
}
fun Context.getCourseId(): String {
    var courseId = "151"
    try {
        courseId = PrefManager.getStringValue(CURRENT_COURSE_ID,false, DEFAULT_COURSE_ID)
    }catch (ex:Exception){
        ex.printStackTrace()
        return "151"
    }
    return courseId
}

fun Context.getCurrentActivity(): String {
    var result = "NA"
    try {
        // TODO: Dynamic Module
        //if(AppObjectController.joshApplication.isAppVisible())
            result = ActivityLifecycleCallback.currentActivity::class.java.simpleName
    }catch (ex:Exception){
        ex.printStackTrace()
    }
    return result
}

fun Context.isFreeTrialOrCourseBought(): Boolean {
    var isFreeTrialOrCourseBought = "false"
    val shouldHaveTapAction = when {
        PrefManager.getBoolValue(IS_COURSE_BOUGHT) -> {
            true
        }
        PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false) -> {
            !PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = true)
        }
        else -> {
            false
        }
    }
    try {
        isFreeTrialOrCourseBought = shouldHaveTapAction.toString()
    }catch (ex:Exception){
        return false
    }
    return isFreeTrialOrCourseBought=="true"
}

fun Context.isBlockedOrFreeTrialEnded(): Boolean {
    var result = "false"
    val isBlockedOrFtEnded = when {
        isBlocked() -> true
        PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false) -> {
            PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
        }
        else -> false
    }
    try {
        result = isBlockedOrFtEnded.toString()
    } catch (ex: Exception) {
        return true
    }
    return result == "true"
}

fun Context.getMentorName(): String {
    var mentorName = ""
    try {
        mentorName = if (PrefManager.getStringValue(USER_NAME)!= EMPTY)
            PrefManager.getStringValue(USER_NAME)
        else
            User.getInstance().firstName ?: ""
    } catch (ex: Exception) {
        ex.printStackTrace()
        return ""
    }
    return mentorName
}

fun Context.getMentorProfile(): String {
    var mentorProfile = ""
    try {
        mentorProfile = if (PrefManager.getStringValue(USER_PROFILE)!= EMPTY)
            PrefManager.getStringValue(USER_PROFILE)
        else
            User.getInstance().photo ?: ""
    }catch (ex: Exception) {
        ex.printStackTrace()
        return ""
    }
    return mentorProfile
}

fun Context.getDeviceId(): String {
    var deviceId = ""
    try {
        deviceId = com.joshtalks.joshskills.premium.core.Utils.getDeviceId()
    } catch (ex: Exception) {
        ex.printStackTrace()
        return ""
    }
    return deviceId
}


//fun Context.updateIncomingCallDetails() {
//    voipLog?.log("QUERY")
//    val values = ContentValues(2).apply {
//        put(CALL_ID, IncomingCallData.callId)
//        put(CALL_TYPE, IncomingCallData.callType)
//    }
//    val data = contentResolver.insert(
//        Uri.parse(CONTENT_URI + INCOMING_CALL_URI),
//        values
//    )
//    voipLog?.log("Data --> $data")
//}

fun getAcceptCallIntent(): PendingIntent {
    val destination = "com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun openCallScreen(): PendingIntent {
    val intent = Intent()
    if (Utils.context?.isBlockedOrFreeTrialEnded() == true) {
        val destination = "com.joshtalks.joshskills.premium.ui.inbox.InboxActivity"
        intent.apply {
            setClassName(Utils.context!!.applicationContext, destination)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    } else {
        val destination = "com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity"
        intent.apply {
            setClassName(Utils.context!!.applicationContext, destination)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            putExtra(INTENT_DATA_COURSE_ID, courseId)
            putExtra(INTENT_DATA_TOPIC_ID, "5")
            putExtra(STARTING_POINT, FROM_ACTIVITY)
        }
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun intentOnNotificationTap(): PendingIntent {
    val destination = "com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        putExtra(STARTING_POINT, FROM_CALL_BAR)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun openFavoriteCallScreen(): PendingIntent {
    val destination = "com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(INTENT_DATA_CALL_CATEGORY, Category.FPP.ordinal)
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}
fun openGroupCallScreen(): PendingIntent {
    val destination = "com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(INTENT_DATA_CALL_CATEGORY, Category.GROUP.ordinal)
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}
fun Context.getHangUpIntent(): PendingIntent {
    val intent = Intent(this, CallingRemoteService::class.java).apply {
        action = SERVICE_ACTION_DISCONNECT_CALL
    }

    return PendingIntent.getService(
        Utils.context,
        1103,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}


fun Context.getTempFileForCallRecording(): File? {
    return File.createTempFile("record", ".aac", this.cacheDir)
}
fun Context.getTempFileForVideoCallRecording(): File? {
    return File.createTempFile("ScreenRecord", ".mp4", this.cacheDir)
}

fun getDeclineCallIntent(): PendingIntent {
    val intent = Intent(Utils.context, CallingRemoteService::class.java).apply {
        action = SERVICE_ACTION_INCOMING_CALL_DECLINE
    }

    return PendingIntent.getService(
        Utils.context,
        1104,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        else
            PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun getRandomName(): String {
    val name = "ABCDFGHIJKLMNOPRSTUVZ"
    val ename = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return name.random().toString().plus(ename.random().toString())
}

fun String.textDrawableBitmap(
    width: Int = 48,
    height: Int = 48,
    bgColor: Int = -1
): Bitmap? {
    val rnd = Random()
    val color = if (bgColor == -1)
        Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    else
        bgColor

    val font = Typeface.createFromAsset(
        Utils.context?.assets,
        "fonts/JoshOpenSans-SemiBold.ttf"
    )
    val drawable = TextDrawable.builder()
        .beginConfig()
        .textColor(Color.WHITE)
        .fontSize(20)
        .useFont(font)
        .toUpperCase()
        .endConfig()
        .buildRound(this, color)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

class Utils {
    companion object {
        @JvmStatic
        var uiHandler: Handler = Handler(Looper.getMainLooper())
            private set
        var context : Application? = null
        val apiHeader : ApiHeader?
            get() = context?.getApiHeader()
        val uuid : String?
            get() = context?.getMentorId()
        val courseId :String?
            get() = context?.getCourseId()
        fun initUtils(application: Application ) {
            context = PremiumApplication.premiumApplication
        }

        fun showToast(msg : String , length: Int = Toast.LENGTH_SHORT){
        Toast.makeText(context,msg,length).show()
        }

        fun isInternetAvailable(): Boolean {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                    // for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                val nwInfo = connectivityManager.activeNetworkInfo ?: return false
                return nwInfo.isConnected
            }
        }

        fun getCurrentTimeStamp(pattern: String = "yyyy-MM-dd HH:mm:ss.SSS"): String {
            val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
            System.currentTimeMillis()
            return dateFormat.format(Date())
        }

//        fun uploadVoipAnalyticsWorker() {
//            val uploadDataConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
//            val workRequest = PeriodicWorkRequestBuilder<UploadAnalyticsWorker>(10, TimeUnit.MINUTES).setConstraints(uploadDataConstraints).build()
//            WorkManager.getInstance(context!!.applicationContext).enqueueUniquePeriodicWork(
//                UPLOAD_ANALYTICS_WORKER_NAME,
//                ExistingPeriodicWorkPolicy.REPLACE,
//                workRequest
//            )
//        }

        fun createPartFromString(descriptionString: String): okhttp3.RequestBody {
            return descriptionString.toRequestBody(okhttp3.MultipartBody.FORM)
        }

        suspend fun syncAnalytics() {
            if (isInternetAvailable()) {
                CallAnalytics.uploadAnalyticsToServer()
                delay(10 * 60 * 1000L)
            } else {
                delay(1 * 60 * 1000L)
            }
            syncAnalytics()
        }

        suspend fun syncCallRecordingAudios() {
            if (isInternetAvailable()) {
                CallRecordingAnalytics.uploadAnalyticsToServer()
                delay(10 * 60 * 1000L)
            } else {
                delay(1 * 60 * 1000L)
            }
            syncCallRecordingAudios()
        }


        inline fun Mutex.onMultipleBackPress(block : () -> Unit) {
            if(this.isLocked) {
                block()
            } else {
                Toast.makeText(context,"Please press back again", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.Main).launch {
                    this@onMultipleBackPress.withLock {
                        delay(1000)
                    }
                }
            }
        }

        inline fun ignoreException(block : ()->Unit) {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun getNotificationTitle(courseId: String, blockedOrFTEnded: Boolean): String {
    val name = Mentor.getInstance().getUser()?.firstName ?: "User"
    if (blockedOrFTEnded) return "${name}, JUST ₹2/DAY !!!"
    return when (courseId) {
        "151", "1214" -> "$name, English बोलने से आती हैं."
        "1203"-> "$name, ইংলিশ প্রাকটিস করলে তবেই বলতে পারবেন।"
        "1206"-> "$name, English ਬੋਲਣ ਨਾਲ ਆਉਂਦੀ ਹੈ।"
        "1207"-> "$name, English बोलल्याने येते."
        "1209"-> "$name, English സംസാരിച്ചു  പഠിക്കാം."
        "1210"-> "$name, பேசினால் தான் ஆங்கிலம் வரும்"
        "1211"-> "$name, English మాట్లాడితేనే వస్తుంది."
        else -> "$name, You will learn English by speaking."
    }
}

private fun getNotificationBody(courseId: String, blockedOrFtEnded: Boolean): String {
    if (!blockedOrFtEnded) return "Call now"
    return when(courseId) {
        "151", "1214" -> "Unlimited Calling, जब चाहें, जहाँ चाहें,  जितना चाहें !!!"
        "1203"-> "Unlimited Calling, যে কোন সময় যে কোন জায়গায় যতটা আপনি চান"
        "1206"-> "Unlimited Calling, ਜਦੋਂ ਚਾਹੋ, ਜਿੱਥੇ ਚਾਹੋ, ਜਿਹਨਾਂ ਚਾਹੋ !!!"
        "1207"-> "Unlimited Calling, केव्हाही, कुठेही, पाहिजे तितके!!!"
        "1209"-> "Unlimited Calling, എപ്പോൾ വേണമെങ്കിലും,എവിടെ വേണമെങ്കിലും,എത്ര വേണമെങ്കിലും!!!"
        "1210"-> "Unlimited Calling, எங்கும், எந்நேரத்திலும், அளவில்லாமல்!!!"
        "1211"-> "Unlimited Calling, ఎప్పుడు కావాలన్న, ఎక్కడ కావాలన్న, ఎంత కావాలన్న !!!"
        else -> "Unlimited Calling, Anytime, Anywhere!!!"
    }
}