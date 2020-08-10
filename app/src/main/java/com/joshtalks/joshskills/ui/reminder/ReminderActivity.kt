package com.joshtalks.joshskills.ui.reminder

import android.os.Bundle
import android.view.View
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity

class ReminderActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)
        val timePicker = findViewById<View>(R.id.time_picker)
    }
}