package com.joshtalks.joshskills.core.notification

import android.os.Bundle
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.tyagiabhinav.dialogflowchatlibrary.ChatbotActivity

class BaseActivityNew : ChatbotActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun logEvent(event: String?) {
        val eventName = when (event) {
            "Yes, Shuru Karein" -> {
                "shuru_button_clicked"
            }
            "Mein ye cheeze achieve karna chahta hoon" -> {
                "achieve_karna_clicked"
            }
            "Ready" -> {
                "ready_button_clicked"
            }
            "Yes" -> {
                "yes_button_clicked"
            }
            "No I want to start my course" -> {
                "start_course_clicked"
            }
            "I want to start my free trial" -> {
                "want_free_trial_clicked"
            }
            "Know about the 1-Year All Course Pass" -> {
                "know_course_pass_clicked"
            }
            "Start Free Trial" -> {
                "start_free_trial_clicked"
            }
            "Not interested, I want to start my free trial" -> {
                "not_interested_clicked"
            }
            else -> {
                event
            }
        }
        AppAnalytics.create(eventName)
            .addUserDetails()
            .addBasicParam()
            .push()
    }
}
