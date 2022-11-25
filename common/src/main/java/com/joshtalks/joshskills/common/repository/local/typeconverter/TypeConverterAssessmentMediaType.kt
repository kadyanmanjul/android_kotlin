package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentMediaType

class TypeConverterAssessmentMediaType {

    @TypeConverter
    fun toAssessmentMediaType(value: String) = enumValueOf<AssessmentMediaType>(value)

    @TypeConverter
    fun fromAssessmentMediaType(value: AssessmentMediaType) = value.name
}
