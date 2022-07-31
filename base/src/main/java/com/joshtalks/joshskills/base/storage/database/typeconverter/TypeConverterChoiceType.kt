package com.joshtalks.joshskills.base.storage.database.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType

class TypeConverterChoiceType {

    @TypeConverter
    fun toChoiceType(value: String) = enumValueOf<ChoiceType>(value)

    @TypeConverter
    fun fromChoiceType(value: ChoiceType) = value.name
}
