package com.joshtalks.joshskills.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus

class TypeConverterAssessmentStatus {

    @TypeConverter
    fun toAssessmentStatus(value: String) = enumValueOf<AssessmentStatus>(value)

    @TypeConverter
    fun fromAssessmentStatus(value: AssessmentStatus) = value.name
}
