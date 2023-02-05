package com.joshtalks.joshskills.premium.repository.local.type_converter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.premium.repository.server.assessment.QuestionStatus

class TypeConverterQuestionStatus {

    @TypeConverter
    fun toQuestionStatus(value: String) = enumValueOf<QuestionStatus>(value)

    @TypeConverter
    fun fromQuestionStatus(value: QuestionStatus) = value.name
}
