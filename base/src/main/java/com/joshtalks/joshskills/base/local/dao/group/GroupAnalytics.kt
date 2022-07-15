package com.joshtalks.joshskills.base.local.dao.group

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.base.local.AppDatabase
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupsAnalyticsEntity
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

private const val TAG = "GroupsAnalytics"

const val GROUPS_ANALYTICS_MENTOR_ID_API_KEY = "mentor_id"
const val GROUPS_ANALYTICS_EVENTS_API_KEY = "group_event_name"
const val GROUPS_ANALYTICS_GROUP_ID_API_KEY = "group_id"

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
        MESSAGE_SENT("MESSAGE_SENT"),
        EXIT_GROUP("EXIT_GROUP"),
        MEMBER_REMOVED_FROM_GROUP("MEMBER_REMOVED_FROM_GROUP"),
        OPENED_PROFILE("OPENED_PROFILE"),
        REQUEST_TO_JOIN("REQUEST_TO_JOIN"),
        OPEN_ADMIN_RESPONSIBILITY("OPEN_ADMIN_RESPONSIBILITY"),
        OPEN_REQUESTS_LIST("OPEN_REQUESTS_LIST"),
        REQUEST_ACCEPTED("REQUEST_ACCEPTED"),
        REQUEST_DECLINED("REQUEST_DECLINED"),
        OPEN_PROFILE_FROM_REQUEST("OPEN_PROFILE_FROM_REQUEST"),
        NOTIFICATION_SENT("NOTIFICATION_SENT"),
        NOTIFICATION_RECEIVED("NOTIFICATION_RECEIVED")
    }

    fun push(
        event: GroupsEvent,
        groupId: String = "",
        mentorId: String = Mentor.getInstance().getId()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val analyticsData = GroupsAnalyticsEntity(
                event.value,
                mentorId,
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
        return mutableMapOf<String, String>().apply {
            this[GROUPS_ANALYTICS_EVENTS_API_KEY] = analyticsData.event
            this[GROUPS_ANALYTICS_MENTOR_ID_API_KEY] = analyticsData.mentorId
            this[GROUPS_ANALYTICS_GROUP_ID_API_KEY] = analyticsData.groupId ?: ""
        }
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
    val value: String
}
