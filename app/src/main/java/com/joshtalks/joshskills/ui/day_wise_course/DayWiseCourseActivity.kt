package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.ChatModel


const val CHAT_OBJECT: String = "CHAT_OBJECT"

open class DayWiseCourseActivity : AppCompatActivity() {


    companion object {
        fun startDayWiseCourseActivity(
            context: Activity,
            requestCode: Int,
            chatModel: ChatModel
        ) {
            val intent = Intent(context, DayWiseCourseActivity::class.java).apply {
                putExtra(CHAT_OBJECT, chatModel)
                //      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                //    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }
    }

    var chatModel: ChatModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.daywise_course_activity
        )
        chatModel = intent.getParcelableExtra(CHAT_OBJECT)
        if (chatModel == null)
            finish()
        NewPracticeActivity.startNewPracticeActivity(this, 101, chatModel!!)
    }
}