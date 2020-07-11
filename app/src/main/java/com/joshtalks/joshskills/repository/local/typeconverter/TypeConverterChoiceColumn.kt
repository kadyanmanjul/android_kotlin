package com.joshtalks.joshskills.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn

class TypeConverterChoiceColumn {

    @TypeConverter
    fun toChoiceColumn(value: String) = enumValueOf<ChoiceColumn>(value)

    @TypeConverter
    fun fromChoiceColumn(value: ChoiceColumn) = value.name
}
