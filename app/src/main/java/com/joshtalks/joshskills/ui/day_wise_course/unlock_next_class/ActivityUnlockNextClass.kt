package com.joshtalks.joshskills.ui.day_wise_course.unlock_next_class

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityUnlockNextClass : AppCompatActivity() {

    private var conversationId = EMPTY

    companion object {
        private val CONVERSATION_ID = "course_id"
        fun getActivityUnlockNextClassIntent(
            context: Context,
            conversationId: String
        ) = Intent(context, ActivityUnlockNextClass::class.java).apply {
            putExtra(CONVERSATION_ID, conversationId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(CONVERSATION_ID).not())
            finish()
        conversationId = intent.getStringExtra(CONVERSATION_ID)

        setContentView(R.layout.acitivity_unlock_next_class_layout)
        findViewById<Button>(R.id.unlock_bt).setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response =
                        AppObjectController.chatNetworkService.changeBatchRequest(conversationId)
                    val arguments = mutableMapOf<String, String>()
                    val (key, value) = PrefManager.getLastSyncTime(conversationId)
                    arguments[key] = value
                    if (response.isSuccessful) {
                        setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, true) })
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }
}