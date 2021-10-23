package com.joshtalks.joshskills.ui.group.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class JoinGroupRequest(
	@field:SerializedName("mentor_id")
	val mentorId: String,

	@field:SerializedName("group_id")
	val groupId: String,
)
