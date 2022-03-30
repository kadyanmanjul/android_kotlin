package com.joshtalks.joshskills.ui.senior_student.data

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.ui.senior_student.model.SeniorStudentModel
import retrofit2.http.GET

interface SeniorStudentService {
    @GET("$DIR/voicecall/senior_student_info/")
    suspend fun getSeniorStudentData(): SeniorStudentModel
}