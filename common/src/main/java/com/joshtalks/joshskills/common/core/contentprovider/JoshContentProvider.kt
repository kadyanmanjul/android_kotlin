package com.joshtalks.joshskills.common.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.common.BuildConfig
import com.joshtalks.joshskills.voip.base.constants.*
import com.joshtalks.joshskills.voip.base.constants.COURSE_ID
import com.joshtalks.joshskills.voip.base.model.ApiHeader
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils.isBlocked
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels.voipLog
import kotlinx.coroutines.sync.Mutex
import kotlin.Exception

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
                    versionName =  AppObjectController.applicationDetails.versionName(),
                    versionCode =  AppObjectController.applicationDetails.versionCode().toString(),
                    userAgent = "APP_" +  AppObjectController.applicationDetails.versionName() + "_" +  AppObjectController.applicationDetails.versionCode().toString(),
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
            CURRENT_ACTIVITY -> {
                val cursor = MatrixCursor(arrayOf(CURRENT_ACTIVITY_COLUMN))
                try {
                    if(AppObjectController.applicationDetails.isAppVisual())
                        cursor.addRow(arrayOf(ActivityLifecycleCallback.currentActivity::class.java.simpleName))
                    else
                        cursor.addRow(arrayOf("NA"))
                } catch (e : Exception) {
                    e.printStackTrace()
                    cursor.addRow(arrayOf("NA"))
                }
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
                Log.d(TAG, "query:IS_COURSE_BOUGHT_OR_FREE_TRIAL ${PrefManager.getBoolValue(IS_COURSE_BOUGHT)} ${PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false)} $shouldHaveTapAction")
                cursor.addRow(arrayOf(shouldHaveTapAction.toString()))
                return cursor
            }
            IS_FT_ENDED_OR_BLOCKED -> {
                val cursor = MatrixCursor(arrayOf(FT_ENDED_OR_BLOCKED_COLUMN))
                val isBlockedOrFtEnded = when {
                    isBlocked() -> true
                    PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false) -> {
                        PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
                    }
                    else -> false
                }
                cursor.addRow(arrayOf(isBlockedOrFtEnded.toString()))
                return cursor
            }
            MENTOR_NAME -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_NAME_COLUMN))
                if (PrefManager.getStringValue(USER_NAME)!= EMPTY)
                    cursor.addRow(arrayOf(PrefManager.getStringValue(USER_NAME)))
                else
                    cursor.addRow(arrayOf(User.getInstance().firstName))
                return cursor
            }
            MENTOR_PROFILE -> {
                val cursor = MatrixCursor(arrayOf(MENTOR_PROFILE_COLUMN))
                if (PrefManager.getStringValue(USER_PROFILE)!= EMPTY)
                    cursor.addRow(arrayOf(PrefManager.getStringValue(USER_PROFILE)))
                else
                    cursor.addRow(arrayOf(User.getInstance().photo))
                return cursor
            }
            DEVICE_ID -> {
                val cursor = MatrixCursor(arrayOf(DEVICE_ID_COLUMN))
                cursor.addRow(arrayOf(Utils.getDeviceId()))
                return cursor
            }
            NOTIFICATION_DATA -> {
                val cursor = MatrixCursor(arrayOf(
                    NOTIFICATION_TITLE_COLUMN,
                    NOTIFICATION_SUBTITLE_COLUMN
                ))
                val isBlockedOrFtEnded = if (PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false)) {
                    when {
                        isBlocked() -> true
                        else -> PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
                    }
                } else false
                return try {
                    cursor.addRow(
                        arrayOf(
                            getNotificationTitle(
                                PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
                                isBlockedOrFtEnded
                            ),
                            getNotificationBody(
                                PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
                                isBlockedOrFtEnded
                            )
                        )
                    )
                    cursor
                } catch (e: Exception) {
                    cursor.addRow(
                        arrayOf(
                            getNotificationTitle(DEFAULT_COURSE_ID, isBlockedOrFtEnded),
                            getNotificationBody(DEFAULT_COURSE_ID, isBlockedOrFtEnded)
                        )
                    )
                    cursor
                }
            }
        }
        return null
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