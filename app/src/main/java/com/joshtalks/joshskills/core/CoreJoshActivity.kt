package com.joshtalks.joshskills.core

import android.app.Activity
import android.content.Intent
import android.view.View
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_WITHOUT_CODE


abstract class CoreJoshActivity : BaseActivity() {
    protected var countUpTimer = CountUpTimer(true)
    protected var videoViewGraphList: MutableSet<Graph> = mutableSetOf<Graph>()
    protected var graph: Graph? = null
    protected var currentAudio: String? = null
    protected var cAudioId: String? = null


    override fun onStart() {
        super.onStart()
        try {
            findViewById<View>(R.id.iv_help).setOnClickListener {
                val i = Intent(this, HelpActivity::class.java)
                startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
                AppAnalytics.create(AnalyticsEvent.HELP_SELECTED.NAME).push()
            }
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

    protected fun endAudioEngagePart(endTime:Long) {
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

}

/*object RxBus22 {
    private val publisher = ReplaySubject.create<Any>()

    @JvmStatic
    fun publish(event: Any) {
        publisher.onNext(event)
    }
    val connectObserable = publisher.share().replay()

}
*/



