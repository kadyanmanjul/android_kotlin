package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class WebrtcViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val repository: ABTestRepository by lazy { ABTestRepository() }
    val fppDialogShow :MutableLiveData<List<String>> = MutableLiveData()
    var isApiFired = false
    val topicUrlLiveData: MutableLiveData<String> = MutableLiveData()

    fun initMissedCall(partnerId: String, aFunction: (String, String, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                requestParams["partner_id"] = partnerId
                requestParams["course_id"] = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                val response =
                    AppObjectController.p2pNetworkService.getFavoriteUserAgoraToken(requestParams)
                if (response.isSuccessful && response.code() in 200..203) {
                    response.body()?.let {
                        aFunction.invoke(
                            it["token"]!!,
                            it["channel_name"]!!,
                            it["uid"]!!.toInt()
                        )
                        PrefManager.put(GET_CALL_ID,it["agora_call_id"]!!)
                    }
                } else if (response.code() == 204) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.INVALIDED)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Exception) {
                when (ex) {
                    is ProtocolException, is HttpException -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.INVALIDED)
                    }
                    else -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                    }
                }
            }
        }
    }
    fun saveIntroVideoFlowImpression(eventName : String, eventDuration : Long = 0L) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName),
                    Pair("duration", eventDuration)
                )
                AppObjectController.commonNetworkService.saveIntroVideoFlowImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    val props = JSONObject()
                    props.put("Variant", data?.variantKey ?: EMPTY)
                    props.put("Variable", AppObjectController.gsonMapper.toJson(data?.variableMap))
                    props.put("Campaign", campaign)
                    props.put("Goal", goal)
                    MixPanelTracker().publishEvent(goal, props)
                }
            }
        }
    }

    fun checkShowFppDialog(map: HashMap<String, String?>){
        if (isApiFired)
            return
        isApiFired = true
        try {
            viewModelScope.launch(Dispatchers.IO){
                try {
                    var resp = AppObjectController.p2pNetworkService.showFppDialog(map).body()
                    var listRes : List<String> = listOf(resp?.get("show_fpp_dialog") ?: EMPTY, resp?.get("show_rating_popup") ?: EMPTY)
                    withContext(Dispatchers.Main){
                        fppDialogShow.value = listRes
                    }
                }catch (ex: Exception) {
                    when (ex) {
                        is HttpException -> {
                            showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                        }
                        is SocketTimeoutException, is UnknownHostException -> {
                            showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                        }
                        else -> {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }
                    }
                }
            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun saveTopicImpression(map: HashMap<String, Any?>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.p2pNetworkService.saveTopicUrlImpression(map)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun showTopicUrl(partnerMentorId:String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topicUrl = AppObjectController.p2pNetworkService.getTopicImage(partnerMentorId)
                withContext(Dispatchers.Main) {
                    topicUrlLiveData.value = topicUrl["topic_url"]
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}