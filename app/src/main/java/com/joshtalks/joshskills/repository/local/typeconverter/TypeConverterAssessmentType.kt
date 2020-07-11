package com.joshtalks.joshskills.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType

class TypeConverterAssessmentType {

    @TypeConverter
    fun toAssessmentType(value: String) = enumValueOf<AssessmentType>(value)

    @TypeConverter
    fun fromAssessmentType(value: AssessmentType) = value.name
}
