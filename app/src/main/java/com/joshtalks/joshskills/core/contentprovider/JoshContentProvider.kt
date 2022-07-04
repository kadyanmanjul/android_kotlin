package com.joshtalks.joshskills.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.constants.COURSE_ID
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import kotlinx.coroutines.sync.Mutex
import java.lang.Exception

private const val TAG = "JoshContentProvider"

class JoshContentProvider : ContentProvider() {
    val mutex = Mutex(false)

    override fun onCreate(): Boolean {
        voipLog?.log("On Create Content Provider $context")
        context?.let { VoipPref.initVoipPref(it) }
        Log.d(TAG, "onCreate: ")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when (uri.path) {
            API_HEADER -> {
                val apiHeader = ApiHeader(
                    token = "JWT " + PrefManager.getStringValue(API_TOKEN),
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE.toString(),
                    userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
                    acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
                )

                val cursor = MatrixCursor(
                    arrayOf(
                        AUTHORIZATION,
                        APP_VERSION_NAME,
                        APP_VERSION_CODE,
                        APP_USER_AGENT,
                        APP_ACCEPT_LANGUAGE
                    )
                )
                cursor.addRow(
                    arrayOf(
                        apiHeader.token,
                        apiHeader.versionName,
                        apiHeader.versionCode,
                        apiHeader.userAgent,
                        apiHeader.acceptLanguage
                    )
                )
                Log.d(TAG, "query: Api Header --> $apiHeader")
                return cursor
            }
            MENTOR_ID -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_ID_COLUMN))
                cursor.addRow(arrayOf(Mentor.getInstance().getId()))
                return cursor
            }
            COURSE_ID -> {
                val cursor = MatrixCursor(arrayOf(COURSE_ID_COLUMN))
                cursor.addRow(arrayOf(PrefManager.getStringValue(CURRENT_COURSE_ID,false, DEFAULT_COURSE_ID)))
                return cursor
            }

            IS_COURSE_BOUGHT_OR_FREE_TRIAL -> {
                val cursor = MatrixCursor(arrayOf(FREE_TRIAL_OR_COURSE_BOUGHT_COLUMN))
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
                cursor.addRow(arrayOf(shouldHaveTapAction.toString()))
                return cursor
            }

            MENTOR_NAME -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_NAME_COLUMN))
                cursor.addRow(arrayOf(User.getInstance().firstName))
                return cursor
            }
            MENTOR_PROFILE -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_PROFILE_COLUMN))
                cursor.addRow(arrayOf(User.getInstance().photo))
                return cursor
            }
            RECORDING_TEXT -> {
                val cursor = MatrixCursor(arrayOf(RECORDING_TEXT_COLUMN))
                val toastText  = AppObjectController.getFirebaseRemoteConfig().getString("RECORDING_SAVED_TEXT")
                cursor.addRow(arrayOf(toastText))
                return cursor
            }
            RECORD_VIDEO_URI -> {
                val cursor = MatrixCursor(arrayOf(VIDEO_COLUMN))
                AppDirectory.videoSentFile().let { file ->
                    cursor.addRow(arrayOf(file.absolutePath))
                }
                return cursor
            }
            NOTIFICATION_DATA -> {
                val cursor =
                    MatrixCursor(arrayOf(NOTIFICATION_TITLE_COLUMN, NOTIFICATION_SUBTITLE_COLUMN,
                        NOTIFICATION_LESSON_COLUMN))
                try{
                    val word = AppObjectController.appDatabase.lessonQuestionDao().getRandomWord()
                    Log.d(TAG, "query: Word ---> ${word.filter { it.word != null }}")
                    cursor.addRow(
                        arrayOf(
                            word.last { it.word != null }.word?: "Undertake",
                            "Practice word of the day",
                            word.last { it.word != null }.lessonId?: 21,
                        )
                    )
                    return cursor
                }catch (e : Exception){
                    cursor.addRow(
                        arrayOf(
                            "Appreciate",
                            "Practice word of the day",
                             21,
                        )
                    )
                    return cursor
                }
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        voipLog?.log("INSERT")
        when (uri.path) {
            START_CALL_TIME_URI -> {
                val startCallTimestamp = values?.getAsLong(CALL_START_TIME) ?: 0L
                VoipPref.updateCurrentCallStartTime(startCallTimestamp)
            }
            CALL_DISCONNECTED_URI -> {
                val duration = values?.getAsLong(CALL_DURATION) ?: 0L
                val remoteUserName = values?.getAsString(REMOTE_USER_NAME) ?: ""
                val remoteUserImage = values?.getAsString(REMOTE_USER_IMAGE)
                val remoteAgoraId = values?.getAsInteger(REMOTE_USER_AGORA_ID) ?: -1
                val callId = values?.getAsInteger(CALL_ID) ?: -1
                val callType = values?.getAsInteger(CALL_TYPE) ?: -1
                val topicName = values?.getAsString(TOPIC_NAME) ?: ""
                val channelName = values?.getAsString(CHANNEL_NAME) ?: ""
                val currentUserAgoraId = values?.getAsInteger(CURRENT_USER_AGORA_ID) ?: -1
                val fppFlag = values?.getAsString(FPP_SHOW_FLAG) ?: "true"
                val remoteUserMentorId = values?.getAsString(REMOTE_USER_MENTOR_ID) ?: ""


                VoipPref.updateLastCallDetails(
                    duration = duration,
                    remoteUserImage = remoteUserImage,
                    remoteUserName = remoteUserName,
                    remoteUserAgoraId = remoteAgoraId,
                    callId = callId,
                    callType = callType,
                    localUserAgoraId = currentUserAgoraId,
                    channelName = channelName,
                    topicName = topicName,
                    showFpp = fppFlag,
                    remoteUserMentorId = remoteUserMentorId
                )
            }
        }
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}