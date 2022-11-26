package com.joshtalks.joshskills.common.core

import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.joshtalks.joshskills.common.R


abstract class ThemedBaseActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWhiteStatusBar()
    }
    private fun setWhiteStatusBar(){
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.pure_white)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            val windowInsetController =  WindowCompat.getInsetsController(window, window.decorView)
            windowInsetController.isAppearanceLightStatusBars = true
        }
    }
}