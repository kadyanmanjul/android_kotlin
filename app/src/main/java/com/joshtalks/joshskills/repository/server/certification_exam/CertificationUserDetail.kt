package com.joshtalks.joshskills.repository.server.certification_exam

import com.google.gson.annotations.SerializedName

data class CertificationUserDetail(
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("father_name")
    val fatherName: String?,
    @SerializedName("mother_name")
    val motherName: String?,
    @SerializedName("mobile")
    val mobile: String?,
    @SerializedName("pin_code")
    val pinCode:Int?,
    @SerializedName("house_number")
    val houseNumber:String?,
    @SerializedName("road_name")
    val roadName:String?,
    @SerializedName("landmark")
    val landmark:String?,
    @SerializedName("town")
    val town:String?,
    @SerializedName("state")
    val state:String?,
    @SerializedName("report_id")
    val reportId: Int? = 0
)
