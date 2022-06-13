package com.joshtalks.joshskills.voip

import android.app.Application
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.joshtalks.joshskills.base.constants.API_HEADER
import com.joshtalks.joshskills.base.constants.APP_ACCEPT_LANGUAGE
import com.joshtalks.joshskills.base.constants.APP_USER_AGENT
import com.joshtalks.joshskills.base.constants.APP_VERSION_CODE
import com.joshtalks.joshskills.base.constants.APP_VERSION_NAME
import com.joshtalks.joshskills.base.constants.AUTHORIZATION
import com.joshtalks.joshskills.base.constants.CALL_DISCONNECTED_URI
import com.joshtalks.joshskills.base.constants.CALL_DURATION
import com.joshtalks.joshskills.base.constants.CALL_ID
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.CALL_TYPE
import com.joshtalks.joshskills.base.constants.CHANNEL_NAME
import com.joshtalks.joshskills.base.constants.CONTENT_URI
import com.joshtalks.joshskills.base.constants.CURRENT_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.MENTOR_ID
import com.joshtalks.joshskills.base.constants.MENTOR_ID_COLUMN
import com.joshtalks.joshskills.base.constants.NOTIFICATION_DATA
import com.joshtalks.joshskills.base.constants.NOTIFICATION_LESSON_COLUMN
import com.joshtalks.joshskills.base.constants.NOTIFICATION_SUBTITLE_COLUMN
import com.joshtalks.joshskills.base.constants.NOTIFICATION_TITLE_COLUMN
import com.joshtalks.joshskills.base.constants.REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.REMOTE_USER_MENTOR_ID
import com.joshtalks.joshskills.base.constants.REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_DISCONNECT_CALL
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_INCOMING_CALL_DECLINE
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_URI
import com.joshtalks.joshskills.base.constants.TOPIC_NAME
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.base.model.NotificationData
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.recordinganalytics.CallRecordingAnalytics
import com.joshtalks.joshskills.voip.voipanalytics.CallAnalytics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody

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
    callType: Int,
    remoteUserAgoraId: Int,
    localUserAgoraId: Int,
    channelName: String,
    topicName: String,
) {
    Log.d(TAG, "updateStartCallTime: ")
    val values = ContentValues(9).apply {
        put(CALL_DURATION, duration)
        put(REMOTE_USER_NAME, remoteUserName)
        put(REMOTE_USER_IMAGE, remoteUserImage)
        put(REMOTE_USER_AGORA_ID, remoteUserAgoraId)
        put(CALL_ID, callId)
        put(CALL_TYPE, callType)
        put(CHANNEL_NAME, channelName)
        put(TOPIC_NAME, topicName)
        put(CURRENT_USER_AGORA_ID, localUserAgoraId)
        put(REMOTE_USER_MENTOR_ID, remotesUserMentorId)

    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + CALL_DISCONNECTED_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.updateStartTime(startTime : Long) {
    Log.d(TAG, "updateStartCallTime: $startTime")
    val values = ContentValues(1).apply { put(CALL_START_TIME, startTime) }
    contentResolver.insert(Uri.parse(CONTENT_URI + START_CALL_TIME_URI), values)
}

suspend fun showToast(msg: String) {
    withContext(Dispatchers.Main) {
        currentToast?.cancel()
        currentToast =  Toast.makeText(Utils.context, msg, Toast.LENGTH_SHORT)
        currentToast?.show()
    }
}

fun Context.getApiHeader(): ApiHeader {
    try {
        val apiDataCursor = contentResolver.query(
            Uri.parse(CONTENT_URI + API_HEADER),
            null,
            null,
            null,
            null
        )

        apiDataCursor?.moveToFirst()
        val apiHeader = ApiHeader(
            token = apiDataCursor.getStringData(AUTHORIZATION),
            versionCode = apiDataCursor.getStringData(APP_VERSION_CODE),
            versionName = apiDataCursor.getStringData(APP_VERSION_NAME),
            userAgent = apiDataCursor.getStringData(APP_USER_AGENT),
            acceptLanguage = apiDataCursor.getStringData(APP_ACCEPT_LANGUAGE)
        )

        apiDataCursor?.close()
        return apiHeader
    } catch (e : Exception) {
        return ApiHeader.empty()
    }
}

fun Context.getNotificationData(): NotificationData {
    try {
        val notificationDataCursor = contentResolver.query(
            Uri.parse(CONTENT_URI + NOTIFICATION_DATA),
            null,
            null,
            null,
            null
        )

        notificationDataCursor?.moveToFirst()
        val notificationData = NotificationData(
            title = notificationDataCursor.getStringData(NOTIFICATION_TITLE_COLUMN),
            subTitle = notificationDataCursor.getStringData(NOTIFICATION_SUBTITLE_COLUMN),
            lessonId = notificationDataCursor.getStringData(NOTIFICATION_LESSON_COLUMN).toInt(),
            )
        notificationDataCursor?.close()
        return notificationData
    } catch (e : Exception) {
        e.printStackTrace()
        return NotificationData.default()
    }
}

private fun Cursor?.getStringData(columnName : String) : String {
    return this?.getString(this.getColumnIndex(columnName)) ?: throw NoSuchElementException("$columnName is NULL from cursor")
}

fun Context.getMentorId(): String {
    val mentorIdCursor = contentResolver.query(
        Uri.parse(CONTENT_URI + MENTOR_ID),
        null,
        null,
        null,
        null
    )

    mentorIdCursor?.moveToFirst()
    val mentorId = mentorIdCursor.getStringData(MENTOR_ID_COLUMN)
    mentorIdCursor?.close()
    return mentorId
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

fun openCallScreen(): PendingIntent {
    val destination = "com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
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
        PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun Context.getTempFileForCallRecording(): File? {
    return File.createTempFile("record", ".aac", this.cacheDir)
}

fun getDeclineCallIntent(): PendingIntent {
    val intent = Intent(Utils.context, CallingRemoteService::class.java).apply {
        action = SERVICE_ACTION_INCOMING_CALL_DECLINE
    }

    return PendingIntent.getService(
        Utils.context,
        1104,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT
    )
}

class Utils {
    companion object {
        var context : Application? = null
        val apiHeader : ApiHeader?
            get() = context?.getApiHeader()
        val uuid : String?
            get() = context?.getMentorId()

        fun initUtils(application: Application ) {
            this.context = application
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