package com.joshtalks.joshskills.premium.calling.recordinganalytics

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.premium.calling.Utils
import com.joshtalks.joshskills.premium.calling.data.AmazonPolicyResponse
import com.joshtalks.joshskills.premium.calling.data.api.CallRecordingRequest
import com.joshtalks.joshskills.premium.calling.data.api.MediaDUNetwork
import com.joshtalks.joshskills.premium.calling.data.api.VoipNetwork
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import com.joshtalks.joshskills.premium.calling.data.local.VoipDatabase
import com.joshtalks.joshskills.premium.calling.recordinganalytics.data.local.RecordingAnalyticsEntity
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import timber.log.Timber

private const val TAG = "CallRecordingAnalytics"

object CallRecordingAnalytics : CallRecordingAnalyticsInterface {

    private val database by lazy {
        Utils.context?.let { VoipDatabase.getDatabase(it.applicationContext) }
    }

    override fun addAnalytics(
        agoraMentorId: String,
        agoraCallId: String?,
        localPath : String
    ) {
        val callEvent = CallRecordingEvents(
            timestamp = Utils.getCurrentTimeStamp(),
            agoraCallId = agoraCallId,
            agoraMentorId = agoraMentorId,
            localPath = localPath
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pushRecordingToServer(callEvent)
            } catch (e: Exception) {
                e.printStackTrace()
                saveLocalFile(callEvent)
            }
        }
    }

    private suspend fun pushRecordingToServer(callEvent: CallRecordingEvents) {
        val recordFile = File(callEvent.localPath)
        if (recordFile==null || recordFile.length() < 0){
            return
        }
        val obj = mapOf("media_path" to recordFile.name)
        val responseObj = requestUploadMediaAsync(obj).await()
        val statusCode: Int = uploadOnS3Server(responseObj, recordFile)
        if (statusCode in 200..210) {
            val url =
                responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
            if (url.isBlank().not()) {
                val request = postCallRecordingFile(
                    CallRecordingRequest(
                        recording_url = url,
                        agoraCallId = PrefManager.getAgraCallId().toString(),
                        agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                    )
                )
                if (request.isSuccessful){
                    val analyticsData = RecordingAnalyticsEntity(
                        agora_call = callEvent.agoraCallId ?: "",
                        agora_mentor = callEvent.agoraMentorId ?: "",
                        localPath = callEvent.localPath,
                        isSync = true,
                        serverPath = url
                    )
                    database?.recordingAnalyticsDao()?.saveAnalytics(analyticsData)
                }
            }
        }
    }

    override suspend fun uploadAnalyticsToServer() {
        pushAnalyticsToServer()
    }

    private suspend fun pushAnalyticsToServer() {
        val analyticsList = database?.recordingAnalyticsDao()?.getUnsyncRecording()
        if (analyticsList != null) {
            for (analytics in analyticsList) {
                try {
                    pushToServer(analytics)
                } catch (e: Exception) {
                    Timber.tag(TAG).e("Error Occurred")
                    e.printStackTrace()
                    if (e is CancellationException)
                        throw e
                }
            }
        }
    }

    private fun saveLocalFile(event: CallRecordingEvents) {
        if (event.agoraCallId?.isEmpty() == true || event.agoraMentorId?.isEmpty() == true || event.localPath.isBlank().not()) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val analyticsData = RecordingAnalyticsEntity(
                    agora_call = event.agoraCallId ?: "",
                    agora_mentor = event.agoraMentorId ?: "",
                    localPath = event.localPath
                )
                database?.recordingAnalyticsDao()?.saveAnalytics(analyticsData)
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private suspend fun pushToServer(entity: RecordingAnalyticsEntity) {
        val recordFile = File(entity.localPath)
        if (recordFile==null || recordFile.length() < 0){
            return
        }
        val obj = mapOf("media_path" to recordFile.name)
        val responseObj = requestUploadMediaAsync(obj).await()
        val statusCode: Int = uploadOnS3Server(responseObj, recordFile)
        if (statusCode in 200..210) {
            val url =
                responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
            if (url.isBlank().not()) {
                val request = postCallRecordingFile(
                    CallRecordingRequest(
                        recording_url = url,
                        agoraCallId = PrefManager.getAgraCallId().toString(),
                        agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
                    )
                )
                if (request.isSuccessful){
                    database?.recordingAnalyticsDao()?.updateSyncStatus(entity.id,url)
                }
            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        recordedFile: File
    ): Int {
        return CoroutineScope(Dispatchers.IO).async {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = recordedFile.asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = MediaDUNetwork.getMediaDUNetworkService().uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    private fun <T> T.serializeToMap(): Map<String, Any> {
        return convert()
    }

    private inline fun <I, reified O> I.convert(): O {
        val gson = Gson()
        val json = gson.toJson(this)
        return gson.fromJson(json, object : TypeToken<O>() {}.type)
    }

    @JvmSuppressWildcards
    private suspend fun requestUploadMediaAsync(params: Map<String, String>): Deferred<AmazonPolicyResponse> {
        return VoipNetwork.getVoipAnalyticsApi().requestUploadMediaAsync(params)
    }
    @JvmSuppressWildcards
    private suspend fun postCallRecordingFile(request : CallRecordingRequest): Response<Unit> {
        return VoipNetwork.getVoipAnalyticsApi().postCallRecordingFile(request)
    }

}
