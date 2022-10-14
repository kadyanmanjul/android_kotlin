package com.joshtalks.joshskills.core

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R

abstract class ThemedCoreJoshActivity: CoreJoshActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)
    }
}