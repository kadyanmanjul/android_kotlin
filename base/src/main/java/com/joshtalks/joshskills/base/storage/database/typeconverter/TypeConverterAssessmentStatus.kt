package com.joshtalks.joshskills.base.storage.database.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus

class TypeConverterAssessmentStatus {

    @TypeConverter
    fun toAssessmentStatus(value: String) = enumValueOf<AssessmentStatus>(value)

    @TypeConverter
    fun fromAssessmentStatus(value: AssessmentStatus) = value.name
}
