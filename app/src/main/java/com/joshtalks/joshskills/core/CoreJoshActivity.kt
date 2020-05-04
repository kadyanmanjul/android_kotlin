package com.joshtalks.joshskills.core

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.feedback.FeedbackFragment
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_WITHOUT_CODE
import com.joshtalks.joshskills.ui.referral.PromotionDialogFragment
import kotlinx.android.synthetic.main.base_toolbar.*

abstract class CoreJoshActivity : BaseActivity() {
    protected var countUpTimer = CountUpTimer(true)
    protected var videoViewGraphList: MutableSet<Graph> = mutableSetOf()
    protected var graph: Graph? = null
    protected var currentAudio: String? = null
    protected var cAudioId: String? = null

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
            iv_help.setOnClickListener { openHelpActivity() }
        } catch (ex: Exception) {
        }
    }


    protected fun setResult() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    protected fun openCourseExplorerScreen() {
        AppAnalytics.create(AnalyticsEvent.EXPLORE_OPENED.NAME)
            .push()
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_WITHOUT_CODE,
            null, true
        )
        this.finish()
    }

    protected fun endAudioEngagePart(endTime: Long) {
        graph?.endTime = endTime
        graph?.let {
            videoViewGraphList.add(it)
        }
        graph = null
    }

    protected fun engageAudio() {
        if (cAudioId.isNullOrEmpty().not() && videoViewGraphList.isNullOrEmpty().not()) {
            EngagementNetworkHelper.engageAudioApi(cAudioId!!, videoViewGraphList.toList())
        }
        videoViewGraphList.clear()
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
    fun openFeedbackFragment(){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("feedback_fragment_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        FeedbackFragment.newInstance()
            .show(supportFragmentManager, "feedback_fragment_dialog")
    }
}
