package com.joshtalks.joshskills.base.storage.database.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType

class TypeConverterAssessmentType {

    @TypeConverter
    fun toAssessmentType(value: String) = enumValueOf<AssessmentType>(value)

    @TypeConverter
    fun fromAssessmentType(value: AssessmentType) = value.name
}
