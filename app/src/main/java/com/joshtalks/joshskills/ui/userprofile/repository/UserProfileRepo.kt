package com.joshtalks.joshskills.ui.userprofile.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.userprofile.models.UpdateProfilePayload
import java.util.HashMap

class UserProfileRepo {
    private val commService by lazy { AppObjectController.commonNetworkService }
    private val signupNetwork by lazy { AppObjectController.signUpNetworkService }
    private val chatNetwork by lazy { AppObjectController.chatNetworkService }
    private val p2pNetwork by lazy { AppObjectController.p2pNetworkService }


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

    suspend fun updateUserProfileV2(updateProfilePayload: UpdateProfilePayload) =
        signupNetwork.updateUserProfileV2(Mentor.getInstance().getId(), updateProfilePayload)

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

    suspend fun getPreviousProfilePics(mentorId: String) =
        commService.getPreviousProfilePics(mentorId)

    suspend fun getProfileCourses(mentorId: String) = commService.getProfileCourses(mentorId)

    suspend fun getProfileAwards(mentorId: String) = commService.getProfileAwards(mentorId)

    suspend fun getProfileGroups(mentorId: String) = commService.getProfileGroups(mentorId)

    suspend fun getFppStatusInProfile(mentorId: String) =
        commService.getFppStatusInProfile(mentorId)

    suspend fun userProfileSectionImpression(map: HashMap<String, String>) =
        commService.userProfileSectionImpression(map)

    suspend fun engageUserProfileSectionTime(map: HashMap<String, String>) =
        commService.engageUserProfileSectionTime(map)

    suspend fun sendFppRequest(receiverMentorId: String) =
        p2pNetwork.sendFppRequest(receiverMentorId)

    suspend fun deleteFppRequest(receiverMentorId: String) =
        p2pNetwork.deleteFppRequest(receiverMentorId)

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: HashMap<String, String>) =
        p2pNetwork.confirmOrRejectFppRequest(senderMentorId, map)
}
