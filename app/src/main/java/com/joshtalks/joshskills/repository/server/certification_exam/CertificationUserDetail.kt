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
    @SerializedName("is_postal_require")
    val isPostalRequire: Boolean,
    @SerializedName("mobile")
    val mobile: String?,
    @SerializedName("postal_address")
    val postalAddress: String?,
    @SerializedName("report_id")
    val reportId: Int? = 0
)
