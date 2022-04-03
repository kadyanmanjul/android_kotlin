package com.joshtalks.joshskills.ui.userprofile

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Message
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.INVITE_FRIENDS_METHOD
import com.joshtalks.joshskills.constants.ON_BACK_PRESS
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.userprofile.repository.ShareFromProfileRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import id.zelory.compressor.Compressor
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.*

const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"
class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val userProfileUrl: MutableLiveData<String?> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val animatedLeaderBoardData: MutableLiveData<AnimatedLeaderBoardResponse> = MutableLiveData()
    val previousProfilePics: MutableLiveData<PreviousProfilePictures> = MutableLiveData()

    val profileRepository: ShareFromProfileRepository by lazy { ShareFromProfileRepository() }
    private var userReferralCode = Mentor.getInstance().referralCode
    var context: JoshApplication = getApplication()
    var singleLiveEvent = EventLiveData
    private var message = Message()
    val helpCountAbTestliveData = MutableLiveData<ABTestCampaignData?>()
    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun getHelpCountCampaignData(campaign: String, mentorId:String, intervalType: String?, previousPage: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                helpCountAbTestliveData.postValue(campaign)
            }
            getProfileData(mentorId, intervalType, previousPage)
        }
    }

    fun getMentorData(mentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getAnimatedLeaderBoardData(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    animatedLeaderBoardData.postValue(response.body())
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun patchAwardDetails(awardIds: ArrayList<Int>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String, List<Int>> = HashMap()
                extras["award_mentor_list"] = awardIds
                AppObjectController.commonNetworkService.patchAwardDetails(extras)

            } catch (ex: Exception) {
                //ex.showAppropriateMsg()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach {
            try {
                it.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getUserProfileUrl() = userProfileUrl.value

    fun uploadMedia(mediaPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatus.postValue(ApiCallStatus.START)
            val compressImagePath = getCompressImage(mediaPath)
            uploadCompressedMedia(compressImagePath)
        }
    }

    private fun uploadCompressedMedia(
        mediaPath: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val obj = mapOf("media_path" to File(mediaPath).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    saveProfileInfo(url)
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun saveProfileInfo(
        profilePicUrl: String?,
        newName: String = EMPTY,
        dobStr: String = EMPTY,
        homeTown: String = EMPTY,
        isSaveBtnClicked: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val requestMap = mutableMapOf<String, String?>()
                requestMap["photo_url"] = profilePicUrl
                if (newName.isNotEmpty()) {
                    requestMap["first_name"] = newName
                }
                if (dobStr.isNotEmpty()) {
                    requestMap["date_of_birth"] = dobStr
                }
                if (homeTown.isNotEmpty()) {
                    requestMap["hometown"] = homeTown
                }
                val response =
                    AppObjectController.signUpNetworkService.updateUserProfile(
                        Mentor.getInstance().getUserId(), requestMap
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        userProfileUrl.postValue(profilePicUrl)
                        if (isSaveBtnClicked) {
                            RxBus2.publish(SaveProfileClickedEvent(true))
                        }
                        it.isVerified = User.getInstance().isVerified
                        User.getInstance().updateFromResponse(it)
                    }
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }
    fun updateProfilePicFromPreviousProfile(imageId : String){

        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.signUpNetworkService.updateProfilePicFromPreviousProfile(imageId)
                if (response.isSuccessful) {
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }
    fun deletePreviousProfilePic(imageId : String){

        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.signUpNetworkService.deletePreviousProfilePic(imageId)
                if (response.isSuccessful) {
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return viewModelScope.async(Dispatchers.IO) {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    private suspend fun getCompressImage(path: String): String {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                AppDirectory.copy(
                    Compressor(getApplication()).setQuality(75).setMaxWidth(720).setMaxHeight(
                        1280
                    ).compressToFile(File(path)).absolutePath, path
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@async path
        }.await()
    }

    fun getProfileData(mentorId: String, intervalType: String?, previousPage: String?) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileDataV3(
                    mentorId,
                    intervalType,
                    previousPage
                )
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)

                    val newList: List<AwardCategory>? =
                        response.body()?.awardCategory?.sortedWith(compareBy { it.sortOrder })
                    newList?.forEach {
                        it.awards = it.awards?.sortedBy { it.sortOrder }
                    }
                    response.body()?.awardCategory = newList
                    userData.postValue(response.body()!!)
                    if (mentorId.equals(Mentor.getInstance().getId()))
                        PrefManager.put(
                            IS_PROFILE_FEATURE_ACTIVE, response.body()?.isPointsActive ?: false
                        )
                    if (mentorId.equals(Mentor.getInstance().getId()))
                        PrefManager.put(USER_SCORE, response.body()!!.points.toString())
                    userProfileUrl.postValue(response.body()!!.photoUrl)
                    return@launch
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.user_does_not_exist))
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun engageUserProfileTime(impressionId: String, startTime: Long) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId.isNullOrBlank())
                    return@launch

                AppObjectController.commonNetworkService.engageUserProfileTime(
                    impressionId,
                    mapOf("time_spent" to startTime)
                )

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getUrlFor3DWebView(awardMentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (awardMentorId.isBlank())
                    return@launch

                val response = AppObjectController.commonNetworkService.get3DWebView(awardMentorId)

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getPreviousProfilePics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.signUpNetworkService.getPreviousProfilePics()
                if (response.isSuccessful) {
                    response.body()?.let {
                        previousProfilePics.postValue(it)
                    }
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }
    fun onBackPress() {
        message.what = ON_BACK_PRESS
        singleLiveEvent.value = message
    }

    fun deepLink(deepLink: String, contentId: String){
        viewModelScope.launch {
            try {
                val requestData = LinkAttribution(
                    mentorId = Mentor.getInstance().getId(),
                    contentId = contentId,
                    sharedItem = "HELP_TIP",
                    sharedItemType = "IM",
                    deepLink = deepLink
                )
                profileRepository.getDeepLink(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }


    fun shareWithFriends(){
        getDeepLinkAndInviteFriends(WHATSAPP_PACKAGE_STRING)
    }

    fun getDeepLinkAndInviteFriends(packageString: String? = null) {
        val referralTimestamp = System.currentTimeMillis()
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(referralTimestamp))
            .setTitle("Invite Friend")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(userReferralCode)
            .setFeature("sharing")
            .setCampaign(userReferralCode.plus(referralTimestamp))
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, userReferralCode)
            .addControlParameter(
                Defines.Jsonkey.UTMCampaign.key,
                userReferralCode.plus(referralTimestamp)
            )
            .addControlParameter(Defines.Jsonkey.UTMMedium.key, "referral")

        branchUniversalObject
            .generateShortUrl(AppObjectController.joshApplication, lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = url
                    )
                else
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl()
                    )
            }
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String) {
        try {
            deepLink(dynamicLink, userReferralCode.plus(System.currentTimeMillis()))

            val waIntent = Intent(Intent.ACTION_SEND)
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }

            waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            waIntent.putExtra(Intent.EXTRA_TEXT, "Mai har roz angrezi mei baat karke angrezi seekh rha hu. Mai chahta hu aap bhi mere saath angrezi seekhe. Is link ko click karke yeh app download kare -\n" + dynamicLink)
            waIntent.type = "text/plain"
            message.what = INVITE_FRIENDS_METHOD
            message.obj = waIntent
            singleLiveEvent.value = message
            postGoal(GoalKeys.HELP_COUNT.name, CampaignKeys.PEOPLE_HELP_COUNT.name)

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(AppObjectController.joshApplication.getString(R.string.whatsApp_not_installed))
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
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

}
