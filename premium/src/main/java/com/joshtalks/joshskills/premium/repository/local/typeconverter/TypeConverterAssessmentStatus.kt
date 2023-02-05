package com.joshtalks.joshskills.premium.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.premium.repository.server.assessment.AssessmentStatus

class TypeConverterAssessmentStatus {

    @TypeConverter
    fun toAssessmentStatus(value: String) = enumValueOf<AssessmentStatus>(value)

    @TypeConverter
    fun fromAssessmentStatus(value: AssessmentStatus) = value.name
}
