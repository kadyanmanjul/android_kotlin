package com.joshtalks.joshskills.base.storage.database.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn

class TypeConverterChoiceColumn {

    @TypeConverter
    fun toChoiceColumn(value: String) = enumValueOf<ChoiceColumn>(value)

    @TypeConverter
    fun fromChoiceColumn(value: ChoiceColumn) = value.name
}
