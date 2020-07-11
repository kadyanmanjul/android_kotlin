package com.joshtalks.joshskills.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType

class TypeConverterChoiceType {

    @TypeConverter
    fun toChoiceType(value: String) = enumValueOf<ChoiceType>(value)

    @TypeConverter
    fun fromChoiceType(value: ChoiceType) = value.name
}
