package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.core.models.FormRequest
import com.joshtalks.badebhaiya.core.models.FormResponse
import com.joshtalks.badebhaiya.core.models.InstallReferrerModel
import com.joshtalks.badebhaiya.core.models.UpdateDeviceRequest
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.datastore.BbDatastore
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.showCallRequests.model.RoomRequestCount
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class CommonRepository {

    private val service = RetrofitInstance.commonNetworkService

    suspend fun postFCMToken(requestParams: Map<String, String>) =
        service.postFCMToken(requestParams)

    suspend fun checkFCMInServer(requestParams: Map<String, String>) =
        service.checkFCMInServer(requestParams)

    suspend fun signOutUser() =service.signOutUser()

    suspend fun patchFCMToken(id: Int, requestParams: Map<String, String>) =
        service.patchFCMToken(id, requestParams)
    fun requestUploadMediaAsync(requestParams: Map<String, String>) = service.requestUploadMediaAsync(requestParams)

    suspend fun sendMsg(params:FormResponse)=service.sendMsg(params)

    suspend fun sendRequest(params:FormRequest)=service.sendRequest(params)

    suspend fun getInstallReferrerAsync(obj: InstallReferrerModel) =
        service.getInstallReferrerAsync(obj)

    suspend fun postDeviceDetails(obj: UpdateDeviceRequest) =
        service.postDeviceDetails(obj)

    suspend fun patchDeviceDetails(deviceId: Int,obj: UpdateDeviceRequest) =
        service.patchDeviceDetails(deviceId,obj)

    val requestsList = flow {
        val response = service.getRequestsList()
        if (response.isSuccessful){
            emit(response.body())
        } else {
            showToast("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)
     .catch {
         showToast("Something Went Wrong")
     }

    suspend fun requestsContent(selectedUserId: String) = flow {
        val response = service.getRequestContent(selectedUserId)
        if (response.isSuccessful){
            emit(response.body())
        } else {
            showToast("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)
     .catch {
         showToast("Something Went Wrong")
     }

    fun getRecordedRoomListeners(roomId: Int) = ListenersPagingSource(roomId)

     fun roomRequestCount() {
//        CoroutineScope(Dispatchers.IO).launch {
//            if (User.getInstance().isSpeaker){
//                try {
//                    val response = service.getRoomRequestCount()
//                    if (response.isSuccessful) {
//                        response.body()?.let {
//                            BbDatastore.updateRoomRequestCount(it.request_count as Long)
////                        return it.request_count
//                        }
//                    }
//                } catch (e: Exception){
//
//                }
//            }
//        }
//        return null
    }
}