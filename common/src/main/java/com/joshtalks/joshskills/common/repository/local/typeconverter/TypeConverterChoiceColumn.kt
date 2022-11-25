package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.ChoiceColumn

class TypeConverterChoiceColumn {

    @TypeConverter
    fun toChoiceColumn(value: String) = enumValueOf<ChoiceColumn>(value)

    @TypeConverter
    fun fromChoiceColumn(value: ChoiceColumn) = value.name
}
