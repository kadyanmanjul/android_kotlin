package com.joshtalks.joshskills.core.notification

import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.tyagiabhinav.dialogflowchatlibrary.ChatbotActivity

class BaseActivityNew : ChatbotActivity() {

    override fun logEvent(event: String?) {
        var eventName = when (event) {
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
            "Start Learning" -> {
                "start_learning_clicked"
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

        if (eventName != null && eventName.contains("thumbnail_enlarge")) {
            eventName = "thumbnail_enlarge"
            AppAnalytics.create(eventName)
                .addUserDetails()
                .addBasicParam()
                .addParam("course name", eventName.substring(17))
                .push()
        } else {

            AppAnalytics.create(eventName)
                .addUserDetails()
                .addBasicParam()
                .push()
        }
    }
}
