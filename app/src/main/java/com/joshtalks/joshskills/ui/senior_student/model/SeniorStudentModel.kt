package com.joshtalks.joshskills.ui.senior_student.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SeniorStudentModel(

	@field:SerializedName("benefits")
	val benefits: List<String>? = null,

	@field:SerializedName("heading")
	val heading: String? = null,

	@field:SerializedName("senior_student")
	val seniorStudent: List<String>? = null
) : Parcelable
