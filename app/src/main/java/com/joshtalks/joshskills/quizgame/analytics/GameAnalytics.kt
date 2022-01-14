package com.joshtalks.joshskills.quizgame.analytics

import android.util.Log
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.analytics.data.GameAnalyticsEntity
import com.joshtalks.joshskills.quizgame.ui.data.network.GAME_ANALYTICS_EVENTS_API_KEY
import com.joshtalks.joshskills.quizgame.ui.data.network.GAME_ANALYTICS_MENTOR_ID_API_KEY
import com.joshtalks.joshskills.quizgame.ui.data.repository.AnalyticsRepo
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

object GameAnalytics {
    private val database by lazy {
        AppDatabase.getDatabase(AppObjectController.joshApplication)
    }
    private val repository by lazy {
        AnalyticsRepo()
    }

    private val mutex = Mutex()

    enum class Event(override val value: String) : GamesEvent {
        CLICK_ON_PLAY_BUTTON("CLICK_ON_PLAY_BUTTON"),
        CLICK_ON_ACCEPT_BUTTON("CLICK_ON_ACCEPT_BUTTON"),
        CLICK_ON_DECLINE_BUTTON("CLICK_ON_DECLINE_BUTTON"),
        OPEN_FPP("OPEN_FPP"),
        OPEN_RANDOM("OPEN_RANDOM"),
        CLICK_ON_MAKE_NEW_TEAM_BUTTON("CLICK_ON_MAKE_NEW_TEAM_BUTTON"),
        CLICK_ON_PLAY_AGAIN_BUTTON("CLICK_ON_PLAY_AGAIN_BUTTON"),
        CLICK_ON_INVITE("CLICK_ON_INVITE"),
        CLICK_ON_ADD_TO_FRIEND_LIST("CLICK_ON_ADD_TO_FRIEND_LIST"),
        CLICK_ON_EXIT("CLICK_ON_EXIT"),
    }

    fun push(event: GamesEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            val analyticsData = GameAnalyticsEntity(
                event.value,
                Mentor.getInstance().getId()
            )
            database?.gameAnalyticsDao()?.saveAnalytics(analyticsData)
            pushToServer()
        }
    }

    fun pushToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val analyticsList = database?.gameAnalyticsDao()?.getAnalytics() ?: mutableListOf()
                for (analytics in analyticsList) {
                    try {
                        val request = getApiRequest(analytics)
                        val response = callAnalyticsApi(request)
                        if (response?.isSuccessful == true){
                            //showToast("Success")
                            Log.d("game_analytics", "pushToServer: $response")
                            database?.gameAnalyticsDao()?.deleteAnalytics(analytics.id)
                        }
                    } catch (e: Exception) {
                        Timber.tag("GAME_ANALYTICS").e("Error Occurred $e")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getApiRequest(analyticsData: GameAnalyticsEntity): Map<String, Any?> {
        val request = mutableMapOf<String, String>().apply {
            this[GAME_ANALYTICS_EVENTS_API_KEY] = analyticsData.event
            this[GAME_ANALYTICS_MENTOR_ID_API_KEY] = analyticsData.mentorId
        }
        return request
    }

    @JvmSuppressWildcards
    private suspend fun callAnalyticsApi(request: Map<String, Any?>) =
        repository.pushAnalyticsToServer(request)

}

interface GamesEvent {
    val value: String
}