package com.joshtalks.joshskills.ui.online_test.util

import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.base.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object A2C1Impressions : A2C1Analytics {
    override fun saveImpression(eventName: Impressions) {
        if (PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID && PrefManager.getBoolValue(
                IS_A2_C1_RETENTION_ENABLED,
                isConsistent = false,
                defValue = false
            )
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AppObjectController.chatNetworkService.saveA2C1Impression(
                        hashMapOf(
                            Pair("mentor_id", Mentor.getInstance().getId()),
                            Pair("event_name", eventName.impressionName)
                        )
                    )
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
            }
        }
    }

    enum class Impressions(val impressionName: String) {
        START_LESSON_CLICKED("START_LESSON_CLICKED"),
        START_LESSON_QUESTIONS("START_LESSON_QUESTIONS"),
        RULE_VIDEO_PLAYED("RULE_VIDEO_PLAYED"),
        RULE_VIDEO_COMPLETED("RULE_VIDEO_COMPLETED"),
    }

}

interface A2C1Analytics {
    fun saveImpression(eventName: A2C1Impressions.Impressions)
}

