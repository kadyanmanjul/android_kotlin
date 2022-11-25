package com.joshtalks.joshskills.common.repository.local.typeconverter

import androidx.room.TypeConverter
import com.joshtalks.joshskills.common.repository.server.assessment.QuestionStatus

class TypeConverterQuestionStatus {

    @TypeConverter
    fun toQuestionStatus(value: String) = enumValueOf<QuestionStatus>(value)

    @TypeConverter
    fun fromQuestionStatus(value: QuestionStatus) = value.name
}
