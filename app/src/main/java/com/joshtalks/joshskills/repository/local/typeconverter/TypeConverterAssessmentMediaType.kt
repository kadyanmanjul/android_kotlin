package com.joshtalks.joshskills.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.AssessmentMediaType

class TypeConverterAssessmentMediaType {

    @TypeConverter
    fun toAssessmentMediaType(value: String) = enumValueOf<AssessmentMediaType>(value)

    @TypeConverter
    fun fromAssessmentMediaType(value: AssessmentMediaType) = value.name
}
