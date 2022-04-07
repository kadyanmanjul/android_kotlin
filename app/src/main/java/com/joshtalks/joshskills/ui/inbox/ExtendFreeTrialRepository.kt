package com.joshtalks.joshskills.ui.inbox

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.delay

const val mentor_id = "mentor_id"
class ExtendFreeTrialRepository {
    private val apiService by lazy { AppObjectController.chatNetworkService }
    private val dbService by lazy { AppObjectController.appDatabase }

    suspend fun extendFreeTrial() =
        apiService.extendFreeTrial(mapOf(mentor_id to Mentor.getInstance().getId()))


    suspend fun getCourseData(): List<InboxEntity>? {
        try {
            val courseListResponse =
                apiService.getRegisteredCourses()
            if (courseListResponse.isEmpty()) {
                return emptyList()
            }
            dbService.courseDao().insertRegisterCourses(courseListResponse)
                .let {
                    return dbService.courseDao().getRegisterCourseMinimal()
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}