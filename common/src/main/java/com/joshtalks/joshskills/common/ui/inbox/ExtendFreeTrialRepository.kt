package com.joshtalks.joshskills.common.ui.inbox

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import kotlinx.coroutines.delay

const val mentor_id = "mentor_id"
class ExtendFreeTrialRepository {
    private val apiService by lazy { AppObjectController.chatNetworkService }
    private val dbService by lazy { AppObjectController.appDatabase }

    suspend fun extendFreeTrial() =
        apiService.extendFreeTrial(mapOf(mentor_id to Mentor.getInstance().getId()))


    suspend fun getCourseData(): List<InboxEntity>? {
        try {
            if (Utils.isInternetAvailable()) {
                val courseListResponse =
                    apiService.getRegisteredCourses()
                if (courseListResponse.isEmpty()) {
                    return emptyList()
                }
                dbService.courseDao().insertRegisterCourses(courseListResponse)
                    .let {
                        return dbService.courseDao().getRegisterCourseMinimal()
                    }
            } else {
                showToast("No internet connection")
                return null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}