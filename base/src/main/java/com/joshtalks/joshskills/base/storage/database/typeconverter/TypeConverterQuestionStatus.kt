package com.joshtalks.joshskills.base.storage.database.typeconverter

import androidx.room.TypeConverter

class TypeConverterQuestionStatus {

    @TypeConverter
    fun toQuestionStatus(value: String) = enumValueOf<QuestionStatus>(value)

    @TypeConverter
    fun fromQuestionStatus(value: QuestionStatus) = value.name
}
