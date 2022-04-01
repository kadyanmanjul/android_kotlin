package com.joshtalks.joshskills.ui.inbox

import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.delay
import java.util.HashMap

class ExtendFreeTrialRepository {
    private val apiService by lazy { AppObjectController.abTestNetworkService }

    suspend fun extendFreeTrial(): Boolean? {
        try {
            val extras: HashMap<String, String> = HashMap()
            extras["mentor_id"] = Mentor.getInstance().getId()
            val response = AppObjectController.chatNetworkService.extendFreeTrial(extras)
            return response.isSuccessful
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
            return null
        }
    }

    suspend fun getCourseData(): List<InboxEntity>? {
        try {
            val courseListResponse =
                AppObjectController.chatNetworkService.getRegisteredCourses()
            if (courseListResponse.isEmpty()) {
                return emptyList()
            }
            AppObjectController.appDatabase.courseDao().insertRegisterCourses(courseListResponse)
                .let {
                    delay(1000)
                    return AppObjectController.appDatabase.courseDao().getRegisterCourseMinimal()
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}