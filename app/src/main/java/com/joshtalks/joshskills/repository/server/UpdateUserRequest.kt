package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality


data class UpdateUserPersonal(
    @SerializedName("first_name")
    var firstName: String = "",
    @SerializedName("date_of_birth")
    var dateOfBirth: String = "",
    @SerializedName("gender")
    var gender: String = ""
)

data class UpdateUserLocality(
    @SerializedName("locality")
    var locality: Locality=Locality()
)