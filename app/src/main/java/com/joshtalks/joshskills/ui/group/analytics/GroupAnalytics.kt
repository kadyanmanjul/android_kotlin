package com.joshtalks.joshskills.ui.group.analytics

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupsAnalyticsEntity
import com.joshtalks.joshskills.ui.group.analytics.data.network.GROUPS_ANALYTICS_EVENTS_API_KEY
import com.joshtalks.joshskills.ui.group.analytics.data.network.GROUPS_ANALYTICS_GROUP_ID_API_KEY
import com.joshtalks.joshskills.ui.group.analytics.data.network.GROUPS_ANALYTICS_MENTOR_ID_API_KEY
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

private const val TAG = "GroupsAnalytics"
object GroupAnalytics {
    // TODO: Inject using Dagger2
    private val database by lazy {
        AppDatabase.getDatabase(AppObjectController.joshApplication)
    }
    private val repository by lazy {
        GroupRepository()
    }

    private val mutex = Mutex()

    enum class Event(override val value: String) : GroupsEvent {
        CREATE_GROUP("CREATE_GROUP"),
        CALL_PRACTICE_PARTNER("CALL_PRACTICE_PARTNER"),
        CALL_PRACTICE_PARTNER_FROM_GROUP("CALL_PRACTICE_PARTNER_FROM_GROUP"),
        OPEN_GROUP_FROM_SEARCH("OPEN_GROUP_SEARCH"),
        OPEN_GROUP_FROM_RECOMMENDATION("OPEN_GROUP_REC"),
        FIND_GROUPS_TO_JOIN("FIND_GROUPS_TO_JOIN"),
        MAIN_GROUP_ICON("MAIN_GROUP_ICON"),
        OPEN_GROUP("OPEN_GROUP"),
        MESSAGE_SENT("MESSAGE_SENT")
    }

    fun push(event: GroupsEvent, groupId: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            val analyticsData = GroupsAnalyticsEntity(
                event.value,
                Mentor.getInstance().getId(),
                groupId
            )
            database?.groupsAnalyticsDao()?.saveAnalytics(analyticsData)
            pushToServer()
        }
    }

    fun pushToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val analyticsList = database?.groupsAnalyticsDao()?.getAnalytics() ?: mutableListOf()
                for (analytics in analyticsList) {
                    try {
                        val request = getApiRequest(analytics)
                        val response = callAnalyticsApi(request)
                        if (response.isSuccessful)
                            database?.groupsAnalyticsDao()?.deleteAnalytics(analytics.id)
                    } catch (e: Exception) {
                        Timber.tag("GROUPS_ANALYTICS").e("Error Occurred")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getApiRequest(analyticsData: GroupsAnalyticsEntity): Map<String, Any?> {
        val request = mutableMapOf<String, String>().apply {
            this[GROUPS_ANALYTICS_EVENTS_API_KEY] = analyticsData.event
            this[GROUPS_ANALYTICS_MENTOR_ID_API_KEY] = analyticsData.mentorId
            this[GROUPS_ANALYTICS_GROUP_ID_API_KEY] = analyticsData.groupId ?: ""
        }

        return request
    }

    fun checkMsgTime(event: GroupsEvent, groupId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val msgSentInDay = repository.getLastSentMsgTime(groupId)
            if (msgSentInDay)
                push(event, groupId)
        }
    }

    @JvmSuppressWildcards
    private suspend fun callAnalyticsApi(request: Map<String, Any?>) = repository.pushAnalyticsToServer(request)

}

interface GroupsEvent {
    val value : String
}
