package com.joshtalks.joshskills.common.core

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker

abstract class CoreJoshActivity : BaseActivity() {

    var currentAudio: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        try {
            findViewById<AppCompatImageView>(R.id.iv_help).setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                openHelpActivity()
            }
        } catch (ex: Throwable) {
            //LogException.catchException(ex)
        }
    }

    fun showPromotionScreen(courseId: String?, placeholderImageUrl: String?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("promotion_show_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)

        // TODO: Use Navigator -- Sahil
//        com.joshtalks.joshskills.referral.PromotionDialogFragment.newInstance(courseId, placeholderImageUrl)
//            .show(supportFragmentManager, "promotion_show_dialog")
        this.intent = null
    }
}