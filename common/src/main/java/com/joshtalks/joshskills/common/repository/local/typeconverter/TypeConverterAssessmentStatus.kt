package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentStatus

class TypeConverterAssessmentStatus {

    @TypeConverter
    fun toAssessmentStatus(value: String) = enumValueOf<AssessmentStatus>(value)

    @TypeConverter
    fun fromAssessmentStatus(value: AssessmentStatus) = value.name
}
