package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.ChoiceType

class TypeConverterChoiceType {

    @TypeConverter
    fun toChoiceType(value: String) = enumValueOf<ChoiceType>(value)

    @TypeConverter
    fun fromChoiceType(value: ChoiceType) = value.name
}
