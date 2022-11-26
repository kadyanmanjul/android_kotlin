package com.joshtalks.joshskills.common.core

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.ui.referral.PromotionDialogFragment
import kotlinx.android.synthetic.main.base_toolbar.iv_help

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
            iv_help.setOnClickListener {
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
        PromotionDialogFragment.newInstance(courseId, placeholderImageUrl)
            .show(supportFragmentManager, "promotion_show_dialog")
        this.intent = null
    }
}
