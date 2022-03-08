package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import java.util.Date
import kotlinx.android.parcel.Parcelize

data class UserProfileResponse(
    @SerializedName("age")
    val age: Int?,
    @SerializedName("createdAt")
    val createdAt: Int?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    @SerializedName("joined_on")
    val joinedOn: String?,
    @SerializedName("lastActiveAt")
    val lastActiveAt: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    var photoUrl: String?,
    @SerializedName("color_code")
    val colorCode: String?,
    @SerializedName("points")
    val points: Int?,
    @SerializedName("minutes_spoken")
    val minutesSpoken: Int?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("streak")
    val streak: Int?,
    @SerializedName("batch_rank")
    val batchRank: Int?,
    @SerializedName("uid")
    val uid: String?,
    @SerializedName("is_points_active")
    val isPointsActive: Boolean? = false,
    @SerializedName("is_container_visible")
    val isContainerVisible: Boolean? = false,
    @SerializedName("is_online")
    val isOnline: Boolean? = false,
    @SerializedName("user_profile_impression_id")
    val userProfileImpressionId: String? = EMPTY,
    @SerializedName("award_category_list")
    var awardCategory: List<AwardCategory>?,
    @SerializedName("certificates")
    val certificates: List<Certificate>?,
    @SerializedName("groups")
    val myGroupsList: List<GroupInfo>?,
    @SerializedName("is_senior_student")
    val isSeniorStudent: Boolean = false,
    @SerializedName("is_course_bought")
    val isCourseBought: Boolean = false,
    @SerializedName("has_group_access")
    val hasGroupAccess: Boolean = false,
    @SerializedName("expire_date")
    val expiryDate: Date? = null,
    @SerializedName("is_conv_room_active")
    val isConvRoomActive: Boolean,
    @SerializedName("hometown")
    val hometown: String? = EMPTY,
    @SerializedName("profile_pictures")
    val previousProfilePictures: PreviousProfilePictures? = null,
    @SerializedName("course_enrolled")
    val enrolledCoursesList: EnrolledCoursesList? = null,
    @SerializedName("is_game_active")
    val isGameActive: Boolean = false,
    @SerializedName("profile_referral_count")
    val numberOfReferral: String,
    @SerializedName("viewer_referral_count")
    val referralOfViewer: String
)

data class GroupInfo(
    @SerializedName("text")
    val textToShow: String?,
    @SerializedName("minutes")
    val minutesSpoken: Int?,
    @SerializedName("name")
    val groupName: String?,
    @SerializedName("icon")
    val groupIcon: String?,

)

data class Certificate(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("certificate_text")
    val certificateText: String?,
    @SerializedName("date_text")
    val dateText: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("certificate_description")
    val certificateDescription: String?,
    @SerializedName("is_achieved")
    val is_achieved: Boolean = false,
    @SerializedName("is_seen")
    val isSeen: Boolean?
)

@Parcelize
data class AwardCategory(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("awards")
    var awards: List<Award>?
) : Parcelable

@Parcelize
data class Award(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("award_text")
    val awardText: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("date_text")
    var dateText:String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("award_description")
    val awardDescription: String?,
    @SerializedName("is_achieved")
    val is_achieved: Boolean = false,
    @SerializedName("is_seen")
    val isSeen: Boolean?,
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("date_list")
    val dateList: List<String?>?,

    var recentDate:String?


) : Parcelable




