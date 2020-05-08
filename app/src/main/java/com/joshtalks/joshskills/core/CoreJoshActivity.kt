package com.joshtalks.joshskills.core

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.server.feedback.FeedbackTypes
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.PRACTISE_SUBMIT_REQUEST_CODE
import com.joshtalks.joshskills.ui.chat.VIDEO_OPEN_REQUEST_CODE
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.feedback.FeedbackFragment
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_WITHOUT_CODE
import com.joshtalks.joshskills.ui.practise.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.referral.PromotionDialogFragment
import com.joshtalks.joshskills.ui.video_player.VIDEO_OBJECT
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    protected fun openFeedbackFragment(feedbackTypes: FeedbackTypes, questionId: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("feedback_fragment_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        FeedbackFragment.newInstance(feedbackTypes, questionId)
            .show(supportFragmentManager, "feedback_fragment_dialog")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE || requestCode == VIDEO_OPEN_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
                    var obj: ChatModel? = null
                    var feedbackType: FeedbackTypes = FeedbackTypes.VIDEO
                    if (data!!.hasExtra(VIDEO_OBJECT)) {
                        obj = data.getParcelableExtra(VIDEO_OBJECT) as ChatModel
                        feedbackType = FeedbackTypes.VIDEO
                    } else if (data.hasExtra(PRACTISE_OBJECT)) {
                        obj = data.getParcelableExtra(PRACTISE_OBJECT) as ChatModel
                        feedbackType = FeedbackTypes.PRACTISE
                    }
                    obj?.question?.questionId?.run {
                        canTakeRequestFeedbackFromUser(feedbackType, this)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun canTakeRequestFeedbackFromUser(
        feedbackTypes: FeedbackTypes,
        questionId: String
    ) {
        val minFeedbackCount =
            AppObjectController.getFirebaseRemoteConfig()
                .getDouble("MINIMUM_FEEDBACK_IN_A_DAY_COUNT").toInt()
        val todaySubmitCount =
            AppObjectController.appDatabase.feedbackEngageModelDao().getTotalCountOfRows()
        if (todaySubmitCount >= minFeedbackCount) {
            return
        }
        val flag =
            AppObjectController.appDatabase.chatDao().getFeedbackStatusOfQuestion(questionId)
        if (flag != null && flag) {
            openFeedbackFragment(feedbackTypes, questionId)
        }

    }
}
