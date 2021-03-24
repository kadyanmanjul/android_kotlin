package com.joshtalks.joshskills.track

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import java.util.concurrent.ExecutorService
import timber.log.Timber

const val CONVERSATION_ID = "conversation_id"
const val SCREEN_NAME = "screen_name"

class CourseUsageService : JobIntentService() {
    private val courseUsageDao = AppObjectController.appDatabase.courseUsageDao()
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Course-Usage-Service")
    private val tag = CourseUsageService::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).e("CREATE")
    }

    override fun onHandleWork(intent: Intent) {
        Timber.tag(tag).e("Handle-Work")
        executor.submit {
            intent.action?.let {
                when (it) {
                    AppUsageStartConversationId().action -> {
                        val conversationId = intent.getStringExtra(CONVERSATION_ID)
                        val screenName = intent.getStringExtra(SCREEN_NAME)
                        val obj = CourseUsageModel(
                            conversationId = conversationId,
                            screenName = screenName
                        )
                        courseUsageDao.insertIntoCourseUsage(obj)
                    }
                    AppUsageEndConversationId().action -> {
                        courseUsageDao.updateLastCourseUsage()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.tag(tag).e("DESTROY")
        super.onDestroy()
    }

    companion object {
        private const val JOB_ID = 1000

        fun startTimeConversation(context: Context, conversationId: String, screenName: String) {
            val intent = Intent().apply {
                action = AppUsageStartConversationId().action
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(SCREEN_NAME, screenName)
            }
            enqueueWork(context, CourseUsageService::class.java, JOB_ID, intent)
        }

        fun endTimeConversation(context: Context, conversationId: String) {
            val intent = Intent().apply {
                action = AppUsageEndConversationId().action
                putExtra(CONVERSATION_ID, conversationId)
            }
            enqueueWork(context, CourseUsageService::class.java, JOB_ID, intent)
        }
    }
}

sealed class AppUsage

data class AppUsageStartConversationId(val action: String = "appusage.start.conversation_id") :
    AppUsage()

data class AppUsageEndConversationId(val action: String = "appusage.end.conversation_id") :
    AppUsage()
