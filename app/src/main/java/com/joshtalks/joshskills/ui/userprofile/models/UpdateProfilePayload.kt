package com.joshtalks.joshskills.ui.userprofile.models

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class UpdateProfilePayload(
    @SerializedName("basic_details")
    var basicDetails: BasicDetails? = BasicDetails(),
    @SerializedName("education_details")
    var educationDetails:EducationDetails?= EducationDetails(),
    @SerializedName("occupation_details")
    var occupationDetails:OccupationDetails?=OccupationDetails()
)
data class BasicDetails (
    @SerializedName("photo_url")
    var photoUrl:String?= EMPTY,
    @SerializedName("first_name")
    var firstName:String?= EMPTY,
    @SerializedName("date_of_birth")
    var dateOfBirth:String?= EMPTY,
    @SerializedName("hometown")
    var homeTown:String?= EMPTY,
    @SerializedName("future_goals")
    var futureGoals:String?= EMPTY,
    @SerializedName("favourite_josh_talk")
    var favouriteJoshTalk:String?= EMPTY
)
data class EducationDetails (
    @SerializedName("degree")
    var degree:String?= EMPTY,
    @SerializedName("college")
    var college:String?= EMPTY,
    @SerializedName("year")
    var year:String?= EMPTY
)
data class OccupationDetails(
    @SerializedName("designation")
    var designation:String?= EMPTY,
    @SerializedName("company")
    var company:String?= EMPTY
)