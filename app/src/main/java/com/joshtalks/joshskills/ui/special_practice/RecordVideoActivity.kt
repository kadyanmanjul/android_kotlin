package com.joshtalks.joshskills.ui.special_practice

import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity

class RecordVideoActivity : CoreJoshActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_video)
        // set a fragment in parent_container
        supportFragmentManager.beginTransaction()
            .replace(R.id.parent_container, RecordVideoFragment()).commit()
    }
}