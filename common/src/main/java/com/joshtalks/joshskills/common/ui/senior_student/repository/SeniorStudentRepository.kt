package com.joshtalks.joshskills.common.ui.senior_student.repository

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.ui.senior_student.model.SeniorStudentModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SeniorStudentRepository {
    val scope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    suspend fun getSeniorStudentData() : SeniorStudentModel {
        return getSeniorStudentDataFromNetwork()
    }

    private suspend fun getSeniorStudentDataFromNetwork() : SeniorStudentModel {
            return try {
                AppObjectController.commonNetworkService.getSeniorStudentData()
            } catch (e : Exception) {
                SeniorStudentModel()
            }
    }
}