package com.joshtalks.joshskills.ui.startcourse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.payment.order_summary.TRANSACTION_ID
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import kotlinx.android.synthetic.main.activity_start_course.*

const val TEACHER_NAME = "teacher_name"
const val IMAGE_URL = "image_url"

class StartCourseActivity : CoreJoshActivity() {

    val isUserRegistered by lazy { Mentor.getInstance().getId().isNotBlank() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_course)
        if (isUserRegistered) {
            materialButton.text = resources.getText(R.string.start)
        } else {
            materialButton.text = resources.getText(R.string.register_now)
        }
        setListeners()
    }

    companion object {
        fun openStartCourseActivity(
            activity: Activity,
            courseName: String,
            teacherName: String,
            imageUrl: String,
            transactionId: Int
        ) {
            Intent(activity, StartCourseActivity::class.java).apply {
                putExtra(COURSE_NAME, courseName)
                putExtra(TEACHER_NAME, teacherName)
                putExtra(IMAGE_URL, imageUrl)
                putExtra(TRANSACTION_ID, transactionId)
            }.run {
                activity.startActivity(this)
            }
        }
    }

    private fun setListeners() {
        if (isUserRegistered) {
            startActivity(getInboxActivityIntent())
            finish()
        } else {
            // TODO(Mohit) - Open Sign Up Activity
        }
    }
}
