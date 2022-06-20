package com.joshtalks.joshskills.ui.userprofile.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Message
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.COURSE_LIST_DATA
import com.joshtalks.joshskills.constants.INVITE_FRIENDS_METHOD
import com.joshtalks.joshskills.constants.MY_GROUP_LIST_DATA
import com.joshtalks.joshskills.constants.ON_BACK_PRESS_PROFILE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.AnimatedLeaderBoardResponse
import com.joshtalks.joshskills.ui.userprofile.adapters.EnrolledCoursesListAdapter
import com.joshtalks.joshskills.ui.userprofile.adapters.MyGroupsListAdapter
import com.joshtalks.joshskills.ui.userprofile.models.*
import com.joshtalks.joshskills.ui.userprofile.repository.UserProfileRepo
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteCallerRepository
import com.joshtalks.joshskills.util.DeepLinkUtil
import com.joshtalks.joshskills.util.showAppropriateMsg
import id.zelory.compressor.Compressor
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val userProfileUrl: MutableLiveData<String?> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val animatedLeaderBoardData: MutableLiveData<AnimatedLeaderBoardResponse> = MutableLiveData()
    val previousProfilePics: MutableLiveData<PreviousProfilePictures> = MutableLiveData()
    val fppList: MutableLiveData<List<FppDetails>> = MutableLiveData()
    val fppRequest: MutableLiveData<FppRequest> = MutableLiveData()
    val groupsList: MutableLiveData<GroupsList> = MutableLiveData()
    val apiCallStatusForGroupsList: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val coursesList: MutableLiveData<EnrolledCoursesList> = MutableLiveData()
    val apiCallStatusForCoursesList: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val awardsList: MutableLiveData<List<AwardCategory>> = MutableLiveData()
    val apiCallStatusForAwardsList: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val sectionImpressionResponse: MutableLiveData<UserProfileSectionResponse> = MutableLiveData()

    private val p2pNetworkService = AppObjectController.p2pNetworkService
    private var mentorId: String = EMPTY
    private val favoriteCallerRepository = FavoriteCallerRepository()
    private var favoriteCallerDao = AppObjectController.appDatabase.favoriteCallerDao()

    private var intervalType: String? = EMPTY
    private var previousPage: String? = EMPTY
    val userProfileRepo = UserProfileRepo()
    val isCourseBought = ObservableBoolean(false)

    val isProgressBarShow = ObservableBoolean(false)
    val enrolledAdapter = EnrolledCoursesListAdapter()
    val myGroupAdapter = MyGroupsListAdapter()
    val isProfileLoading = ObservableBoolean(false)
    private var startTime = System.currentTimeMillis()
    private var impressionId: String? = null

    var message = Message()
    var singleLiveEvent = EventLiveData

    var context: JoshApplication = getApplication()

    val helpCountAbTestliveData = MutableLiveData<ABTestCampaignData?>()
    val repository: ABTestRepository by lazy { ABTestRepository() }
    var count = ObservableField(0)
    fun getHelpCountCampaignData(
        campaign: String,
        mentorId: String,
        intervalType: String?,
        previousPage: String?
    ) {
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
                val response = userProfileRepo.getAnimatedLeaderBoardData(mentorId)
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
                userProfileRepo.patchAwardDetails(extras)

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
                val responseObj = userProfileRepo.requestMediaRequest(obj)
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    var updateProfilePayload = UpdateProfilePayload()
                    var date: String? = null
                    if (userData.value != null) {
                        updateProfilePayload.apply {
                            if (userData.value?.dateOfBirth != null) {
                                date =
                                    DATE_FORMATTER.format(DD_MM_YYYY.parse(userData.value?.dateOfBirth))
                            }
                            basicDetails?.apply {
                                photoUrl = url
                                firstName = userData.value?.name
                                dateOfBirth = date
                                homeTown = userData.value?.hometown
                                futureGoals = userData.value?.futureGoals
                                favouriteJoshTalk = userData.value?.favouriteJoshTalk
                            }
                            educationDetails = null
                            occupationDetails = null
                        }
                    } else {
                        updateProfilePayload.apply {
                            val user = User.getInstance()
                            basicDetails?.apply {
                                photoUrl = url
                                firstName = user.firstName
                                dateOfBirth = user.dateOfBirth
                                homeTown = null
                                futureGoals = null
                                favouriteJoshTalk = null
                            }
                            educationDetails = null
                            occupationDetails = null
                        }
                    }
                    saveProfileInfo(updateProfilePayload)
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
        updateProfilePayload: UpdateProfilePayload?,
        isSaveBtnClicked: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    updateProfilePayload?.let {
                        AppObjectController.signUpNetworkService.updateUserProfileV2(
                            Mentor.getInstance().getUserId(), it
                        )
                    }
                if (response != null) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            userProfileUrl.postValue(updateProfilePayload.basicDetails?.photoUrl)
                            if (isSaveBtnClicked) {
                                RxBus2.publish(SaveProfileClickedEvent(true))
                            }
                            getProfileData(mentorId, intervalType, previousPage)
                        }
                        apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                        return@launch
                    } else {
                        apiCallStatus.postValue(ApiCallStatus.FAILED)
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    fun updateProfilePicFromPreviousProfile(imageId: String, mentorId: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = userProfileRepo.updateProfilePicFromPreviousProfile(imageId)
                if (response.isSuccessful) {
                    getPreviousProfilePics(mentorId)
                    getProfileData(mentorId, intervalType, previousPage)
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

    fun deletePreviousProfilePic(imageId: String, mentorId: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = userProfileRepo.deletePreviousProfilePic(imageId)
                if (response.isSuccessful) {
                    getPreviousProfilePics(mentorId)
                    getProfileData(mentorId, intervalType, previousPage)
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

    fun getProfileAwards(mentorId: String) {
        apiCallStatusForAwardsList.postValue(ApiCallStatus.START)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getProfileAwards(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusForAwardsList.postValue(ApiCallStatus.SUCCESS)
                    val newList: List<AwardCategory>? =
                        response.body()?.awardCategoryList?.sortedWith(compareBy { it.sortOrder })
                    newList?.forEach {
                        it.awards = it.awards?.sortedBy { it.sortOrder }
                    }
                    awardsList.postValue(newList)

                } else {
                    apiCallStatusForAwardsList.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                apiCallStatusForAwardsList.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }

    }

    fun getProfileGroups(mentorId: String) {
        apiCallStatusForGroupsList.postValue(ApiCallStatus.START)
        isProgressBarShow.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getProfileGroups(mentorId)
                if (response.isSuccessful && response.body()?.groupsList?.myGroupsList != null) {
                    apiCallStatusForGroupsList.postValue(ApiCallStatus.SUCCESS)
                    isProgressBarShow.set(false)
                    //groupsList.postValue(response.body()!!.groupsList)
                    withContext(Dispatchers.Main) {
                        message.what = MY_GROUP_LIST_DATA
                        message.obj = response.body()!!.groupsList
                        singleLiveEvent.value = message
                        myGroupAdapter.addMyGroupToList(response.body()?.groupsList?.myGroupsList)
                    }
                } else {
                    apiCallStatusForGroupsList.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                apiCallStatusForGroupsList.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getProfileCourses(mentorId: String) {
        apiCallStatusForCoursesList.postValue(ApiCallStatus.START)
        isProgressBarShow.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getProfileCourses(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    isProgressBarShow.set(false)
                    apiCallStatusForCoursesList.postValue(ApiCallStatus.SUCCESS)
                    ///coursesList.postValue(response.body()!!.enrolledCoursesList)
                    withContext(Dispatchers.Main) {
                        message.what = COURSE_LIST_DATA
                        message.obj = response.body()!!.enrolledCoursesList
                        singleLiveEvent.value = message
                        enrolledAdapter.addEnrolledCoursesToList(response.body()?.enrolledCoursesList?.courses)
                    }
                } else {
                    apiCallStatusForCoursesList.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                apiCallStatusForCoursesList.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getProfileData(mentorId: String, intervalType: String?, previousPage: String?) {
        this.mentorId = mentorId
        this.intervalType = intervalType
        this.previousPage = previousPage
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        isProfileLoading.set(true)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileDataV3(
                    mentorId,
                    intervalType,
                    previousPage
                )
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    userData.postValue(response.body()!!)
                    if (mentorId.equals(Mentor.getInstance().getId()))
                        PrefManager.put(
                            IS_PROFILE_FEATURE_ACTIVE, response.body()?.isPointsActive ?: false
                        )
                    if (mentorId.equals(Mentor.getInstance().getId()))
                        PrefManager.put(USER_SCORE, response.body()!!.points.toString())
                    userProfileUrl.postValue(response.body()!!.photoUrl)
                    isProfileLoading.set(false)
                    return@launch
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.user_does_not_exist))
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            isProfileLoading.set(false)
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun getFppStatusInProfile(mentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getFppStatusInProfile(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    if (response.body()?.fppList!=null) {
                        response.body()?.fppList?.let {
                            fppList.postValue(it)
                        }
                    }
                    if (response.body()?.fppRequest != null) {
                        response.body()?.fppRequest?.let {
                            fppRequest.postValue(it)
                        }
                    }
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    showToast(AppObjectController.joshApplication.getString(R.string.user_does_not_exist))
                } else {
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
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

    fun userProfileSectionImpression(mentorId: String, sectionType: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap<String, String>()
                map["mentor_id"] = mentorId
                map["section_type"] = sectionType
                val response =
                    AppObjectController.commonNetworkService.userProfileSectionImpression(map)
                if (response.success == true && response.sectionImpressionId != null) {
                    impressionId = response.sectionImpressionId
                    sectionImpressionResponse.postValue(response)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun engageUserProfileSectionTime(impressionId: String, timeSpent: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId == null)
                    return@launch
                val map: HashMap<String, String> = HashMap<String, String>()
                map["section_impression_id"] = impressionId
                map["time_spent"] = timeSpent
                AppObjectController.commonNetworkService.engageUserProfileSectionTime(map)

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

    fun sendFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map =  HashMap<String,String>()
                map["page_type"] = "USER_PROFILE"
                p2pNetworkService.sendFppRequest(receiverMentorId,map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.deleteFppRequest(receiverMentorId)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun confirmOrRejectFppRequest(senderMentorId: String, userStatus: String, pageType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap<String, String>()
                map[userStatus] = "true"
                map["page_type"] = pageType
                p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getPreviousProfilePics(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.commonNetworkService.getPreviousProfilePics(mentorId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        previousProfilePics.postValue(it.previousProfilePictures)
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

    fun onBackPress(view: View) {
        message.what = ON_BACK_PRESS_PROFILE
        singleLiveEvent.value = message
    }

    fun saveImpression() {
        try {
            startTime = System.currentTimeMillis().minus(startTime).div(1000)
            if (startTime > 0 && impressionId?.isBlank()?.not() == true) {
                engageUserProfileSectionTime(impressionId!!, startTime.toString())
            }
        } catch (ex: Exception) {
        }
    }

    val onItemClick: (CourseEnrolled, Int) -> Unit = { it, type ->
        when (type) {

        }
    }

    val onGroupItemClick: (GroupInfo, Int) -> Unit = { it, type ->
        when (type) {
            //TODO()
        }
    }

    fun shareWithFriends() {
        getDeepLinkAndInviteFriends(WHATSAPP_PACKAGE_STRING)
    }

    fun getDeepLinkAndInviteFriends(packageString: String? = null) {
        DeepLinkUtil(AppObjectController.joshApplication)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = deepLink
                    )
                }
            })
            .build()
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String) {
        try {
            val waIntent = Intent(Intent.ACTION_SEND)
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }

            waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            waIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Mai har roz angrezi mei baat karke angrezi seekh rha hu. Mai chahta hu aap bhi mere saath angrezi seekhe. Is link ko click karke yeh app download kare -\n$dynamicLink"
            )
            waIntent.type = "text/plain"
            message.what = INVITE_FRIENDS_METHOD
            message.obj = waIntent
            singleLiveEvent.value = message
            postGoal(GoalKeys.HELP_COUNT.name, CampaignKeys.PEOPLE_HELP_COUNT.name)

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(AppObjectController.joshApplication.getString(R.string.whatsApp_not_installed))
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    MixPanelTracker.publishEvent(MixPanelEvent.GOAL)
                        .addParam(ParamKeys.VARIANT, data?.variantKey ?: EMPTY)
                        .addParam(
                            ParamKeys.VARIABLE,
                            AppObjectController.gsonMapper.toJson(data?.variableMap)
                        )
                        .addParam(ParamKeys.CAMPAIGN, campaign)
                        .addParam(ParamKeys.GOAL, goal)
                        .push()
                }
            }
        }
    }

    fun removeFpp(uId: Int) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val requestParams: HashMap<String, List<Int>> = HashMap()
                requestParams["mentor_ids"] = uId.let { return@let listOf(uId) }
                val response = favoriteCallerRepository.removeUserFormFppLit(requestParams)
                if (response.isSuccessful) {
                    favoriteCallerDao.removeFromFavorite(uId.let { return@let listOf(uId) })
                    getFppStatusInProfile(mentorId)
                    getProfileData(mentorId, null, null)
                }
            }
        } catch (ex: Exception) {
        }
    }
}
