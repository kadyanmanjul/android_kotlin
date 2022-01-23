package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class D2pSyllabusPdfResponse(
    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("syllabus")
    val SyllabusPdfLink: String
)
