package com.joshtalks.joshskills.ui.userprofile.viewmodel

import android.app.Application
import android.os.Message
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.DD_MM_YYYY
import com.joshtalks.joshskills.core.DATE_FORMATTER
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.USER_SCORE
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.AnimatedLeaderBoardResponse
import com.joshtalks.joshskills.ui.userprofile.adapters.EnrolledCoursesListAdapter
import com.joshtalks.joshskills.ui.userprofile.adapters.MyGroupsListAdapter
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import com.joshtalks.joshskills.ui.userprofile.models.PreviousProfilePictures
import com.joshtalks.joshskills.ui.userprofile.models.FppDetails
import com.joshtalks.joshskills.ui.userprofile.models.AwardCategory
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileSectionResponse
import com.joshtalks.joshskills.ui.userprofile.models.FppRequest
import com.joshtalks.joshskills.ui.userprofile.models.UpdateProfilePayload
import com.joshtalks.joshskills.ui.userprofile.models.CourseEnrolled
import com.joshtalks.joshskills.ui.userprofile.models.GroupInfo
import com.joshtalks.joshskills.ui.userprofile.repository.UserProfileRepo
import com.joshtalks.joshskills.ui.userprofile.utils.COURSE_LIST_DATA
import com.joshtalks.joshskills.ui.userprofile.utils.MY_GROUP_LIST_DATA
import com.joshtalks.joshskills.ui.userprofile.utils.ON_BACK_PRESS
import com.joshtalks.joshskills.util.showAppropriateMsg
import id.zelory.compressor.Compressor
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

open class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val userProfileUrl: MutableLiveData<String?> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val animatedLeaderBoardData: MutableLiveData<AnimatedLeaderBoardResponse> = MutableLiveData()
    val previousProfilePics: MutableLiveData<PreviousProfilePictures> = MutableLiveData()
    val fppList: MutableLiveData<List<FppDetails>> = MutableLiveData()
    val fppRequest: MutableLiveData<FppRequest> = MutableLiveData()
    val awardsList: MutableLiveData<List<AwardCategory>> = MutableLiveData()
    val apiCallStatusForAwardsList: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val sectionImpressionResponse: MutableLiveData<UserProfileSectionResponse> = MutableLiveData()

    private var mentorId: String = EMPTY
    private var intervalType: String? = EMPTY
    private var previousPage: String? = EMPTY
    val userProfileRepo = UserProfileRepo()
    val isCourseBought = ObservableBoolean(false)

    val isProgressBarShow = ObservableBoolean(false)
    val fetchingGroupList = ObservableBoolean(false)
    val fetchingEnrolledCourseList = ObservableBoolean(false)
    val enrolledAdapter = EnrolledCoursesListAdapter()
    val myGroupAdapter = MyGroupsListAdapter()

    private var startTime = System.currentTimeMillis()
    private var impressionId: String? = null


    private var message = Message()

    private var singleLiveEvent = EventLiveData

    var context: JoshApplication = getApplication()

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
                    updateProfilePayload.apply {
                        val date = DD_MM_YYYY.parse(userData.value?.dateOfBirth)
                        basicDetails?.apply {
                            photoUrl = url
                            firstName = userData.value?.name
                            dateOfBirth = DATE_FORMATTER.format(date)
                            homeTown = userData.value?.hometown
                            futureGoals = userData.value?.futureGoals
                            favouriteJoshTalk = userData.value?.favouriteJoshTalk
                        }
                        educationDetails?.apply {
                            degree = userData.value?.educationDetails?.degree
                            college = userData.value?.educationDetails?.college
                            year = userData.value?.educationDetails?.year
                        }
                        occupationDetails?.apply {
                            designation = userData.value?.occupationDetails?.designation
                            company = userData.value?.occupationDetails?.company
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
                val response =
                    AppObjectController.signUpNetworkService.updateProfilePicFromPreviousProfile(
                        imageId
                    )
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
                val response = userProfileRepo.getProfileAwards(mentorId)
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
        fetchingGroupList.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = userProfileRepo.getProfileGroups(mentorId)
                isProgressBarShow.set(true)
                if (response.isSuccessful && response.body()?.groupsList?.myGroupsList != null) {
                    fetchingGroupList.set(false)
                    isProgressBarShow.set(false)
                    withContext(Dispatchers.Main) {
                        message.what = MY_GROUP_LIST_DATA
                        message.obj = response.body()!!.groupsList
                        singleLiveEvent.value = message
                        myGroupAdapter.addMyGroupToList(response.body()?.groupsList?.myGroupsList)
                    }
                } else {
                    fetchingGroupList.set(false)
                    isProgressBarShow.set(false)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                fetchingGroupList.set(false)
                isProgressBarShow.set(false)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getProfileCourses(mentorId: String) {
        fetchingEnrolledCourseList.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = userProfileRepo.getProfileCourses(mentorId)
                isProgressBarShow.set(true)
                if (response.isSuccessful && response.body() != null) {
                    isProgressBarShow.set(false)
                    fetchingEnrolledCourseList.set(false)
                    withContext(Dispatchers.Main) {
                        message.what = COURSE_LIST_DATA
                        message.obj = response.body()!!.enrolledCoursesList
                        singleLiveEvent.value = message
                        enrolledAdapter.addEnrolledCoursesToList(response.body()?.enrolledCoursesList?.courses)
                    }
                } else {
                    fetchingEnrolledCourseList.set(false)
                    isProgressBarShow.set(false)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                fetchingEnrolledCourseList.set(false)
                isProgressBarShow.set(false)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getProfileData(mentorId: String, intervalType: String?, previousPage: String?) {
        this.mentorId = mentorId
        this.intervalType = intervalType
        this.previousPage = previousPage
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = userProfileRepo.getUserProfileDataV3(
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

    fun getFppStatusInProfile(mentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = userProfileRepo.getFppStatusInProfile(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    if (response.body()!!.fppList.isNullOrEmpty()) {
                        response.body()!!.fppRequest?.let {
                            fppRequest.postValue(it)
                        }
                    } else if (response.body()!!.fppRequest == null) {
                        response.body()!!.fppList?.let {
                            fppList.postValue(it)
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

                userProfileRepo.engageUserProfileTime(impressionId, startTime)

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
                val response = userProfileRepo.userProfileSectionImpression(map)
                if (response.success == true && response.sectionImpressionId != null) {
                    sectionImpressionResponse.postValue(response)
                    impressionId = response.sectionImpressionId
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
                val map: HashMap<String, String> = HashMap()
                map["section_impression_id"] = impressionId
                map["time_spent"] = timeSpent
                userProfileRepo.engageUserProfileSectionTime(map)

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
                userProfileRepo.sendFppRequest(receiverMentorId)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userProfileRepo.deleteFppRequest(receiverMentorId)
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
                userProfileRepo.confirmOrRejectFppRequest(senderMentorId, map)
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
        message.what = ON_BACK_PRESS
        singleLiveEvent.value = message
    }

    fun saveImpression() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId!!.isBlank().not()) {
            engageUserProfileSectionTime(impressionId!!, startTime.toString())
        }
    }

    val onItemClick: (CourseEnrolled, Int) -> Unit = { it, type ->
        when (type) {
            //TODO()
        }
    }

    val onGroupItemClick: (GroupInfo, Int) -> Unit = { it, type ->
        when (type) {
            //TODO()
        }
    }

}
