package com.joshtalks.joshskills.ui.day_wise_course.unlock_next_class

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED

class ActivityUnlockNextClass : AppCompatActivity() {

    private var conversationId = EMPTY

    private lateinit var lessonName: TextView
    private lateinit var lessonIv: ImageView
    private lateinit var descTv: TextView

    private var lessonModel: LessonModel? = null

    companion object {
        private val CONVERSATION_ID = "course_id"
        private val LESSON_MODEL = "lesson_model"
        fun getActivityUnlockNextClassIntent(
            context: Context,
            conversationId: String,
            lessonModel: LessonModel
        ) = Intent(context, ActivityUnlockNextClass::class.java).apply {
            putExtra(CONVERSATION_ID, conversationId)
            putExtra(LESSON_MODEL, lessonModel)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(CONVERSATION_ID).not())
            finish()
        conversationId = intent.getStringExtra(CONVERSATION_ID)

        lessonModel = intent.getParcelableExtra(LESSON_MODEL)

        setContentView(R.layout.acitivity_unlock_next_class_layout)
        lessonName = findViewById(R.id.lesson_name_tv)
        lessonIv = findViewById(R.id.lesson_iv)
        descTv = findViewById(R.id.description_tv)

        lessonModel?.let {
            lessonName.text = it.lessonName
            Utils.setImage(lessonIv, it.varthumbnail)
            /*descTv.text = it.*/
        }

        findViewById<Button>(R.id.continue_btn).setOnClickListener {
            setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, true) })
            finish()
            /* CoroutineScope(Dispatchers.IO).launch {
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
             }*/

        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, true) })
        finish()
        super.onBackPressed()
    }
}