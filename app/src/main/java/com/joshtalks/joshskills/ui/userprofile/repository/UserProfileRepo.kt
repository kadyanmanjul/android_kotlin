package com.joshtalks.joshskills.ui.userprofile.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import java.util.HashMap

class UserProfileRepo {
    private val commService by lazy { AppObjectController.commonNetworkService }
    private val signupNetwork by lazy { AppObjectController.signUpNetworkService }
    private val chatNetwork by lazy { AppObjectController.chatNetworkService }


    suspend fun getAnimatedLeaderBoardData(mentorId: String) =
        commService.getAnimatedLeaderBoardData(mentorId)

    suspend fun updateUserProfile(requestMap: MutableMap<String, String?>) =
        signupNetwork.updateUserProfile(Mentor.getInstance().getUserId(), requestMap)

    suspend fun patchAwardDetails(extras: HashMap<String, List<Int>>) =
        commService.patchAwardDetails(extras)

    suspend fun requestMediaRequest(obj: Map<String, String>) =
        chatNetwork.requestUploadMediaAsync(obj).await()

    suspend fun updateProfilePicFromPreviousProfile(imageId: String) =
        signupNetwork.updateProfilePicFromPreviousProfile(imageId)

    suspend fun deletePreviousProfilePic(imageId: String) =
        signupNetwork.deletePreviousProfilePic(imageId)

    suspend fun getUserProfileDataV3(
        mentorId: String,
        intervalType: String?,
        previousPage: String?
    ) = commService.getUserProfileDataV3(mentorId, intervalType, previousPage)

    suspend fun engageUserProfileTime(impressionId: String, startTime: Long) =
        commService.engageUserProfileTime(
            impressionId,
            mapOf("time_spent" to startTime)
        )

    suspend fun getPreviousProfilePics() = signupNetwork.getPreviousProfilePics()
}
