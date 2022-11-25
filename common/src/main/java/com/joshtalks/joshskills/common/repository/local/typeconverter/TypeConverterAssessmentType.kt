package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentType

class TypeConverterAssessmentType {

    @TypeConverter
    fun toAssessmentType(value: String) = enumValueOf<AssessmentType>(value)

    @TypeConverter
    fun fromAssessmentType(value: AssessmentType) = value.name
}
