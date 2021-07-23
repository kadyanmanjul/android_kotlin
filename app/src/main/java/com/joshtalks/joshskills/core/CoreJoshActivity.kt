package com.joshtalks.joshskills.core

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.repository.server.feedback.FeedbackTypes
import com.joshtalks.joshskills.ui.feedback.FeedbackFragment
import com.joshtalks.joshskills.ui.referral.PromotionDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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

    private fun openFeedbackFragment(feedbackTypes: FeedbackTypes, questionId: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("feedback_fragment_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        FeedbackFragment.newInstance(feedbackTypes, questionId)
            .show(supportFragmentManager, "feedback_fragment_dialog")
    }
/*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch(Dispatchers.IO){
            try {
                if ((requestCode == PRACTISE_SUBMIT_REQUEST_CODE || requestCode == VIDEO_OPEN_REQUEST_CODE) && resultCode == Activity.RESULT_OK && data != null) {
                    var obj: ChatModel? = null
                    var feedbackType: FeedbackTypes = FeedbackTypes.VIDEO
                    val videoWatchTime = TimeUnit.MILLISECONDS.toMinutes(
                        data.getIntExtra(VIDEO_WATCH_TIME, 0).toLong()
                    )
                    if (data.hasExtra(VIDEO_OBJECT)) {
                        obj = data.getParcelableExtra(VIDEO_OBJECT) as ChatModel
                        feedbackType = FeedbackTypes.VIDEO
                    } else if (data.hasExtra(PRACTISE_OBJECT)) {
                        obj = data.getParcelableExtra(PRACTISE_OBJECT) as ChatModel
                        feedbackType = FeedbackTypes.PRACTISE
                    }
                    obj?.question?.questionId?.run {
                        if (canShowNPSDialog(id = this)) {
                            return@launch
                        } else {
                            if (feedbackType == FeedbackTypes.VIDEO && videoWatchTime < 1) {
                                return@launch
                            }
                            if (canTakeRequestFeedbackFromUser(this)) {
                                showFeedback(feedbackType, this)
                            }
                        }
                    }
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }
    }*/

    private suspend fun canTakeRequestFeedbackFromUser(questionId: String): Boolean {
        return withContext(lifecycleScope.coroutineContext + Dispatchers.IO) {
            val minFeedbackCount =
                AppObjectController.getFirebaseRemoteConfig()
                    .getDouble("MINIMUM_FEEDBACK_IN_A_DAY_COUNT").toInt()
            val todaySubmitCount =
                AppObjectController.appDatabase.feedbackEngageModelDao().getTotalCountOfRows()
            if (todaySubmitCount >= minFeedbackCount) {
                return@withContext false
            }
//            val flag =
//                AppObjectController.appDatabase.chatDao().getFeedbackStatusOfQuestion(questionId)
//            if (flag != null && flag) {
//                return@withContext true
//            }
//            return@withContext false
            return@withContext true
        }
    }

    private fun showFeedback(feedbackTypes: FeedbackTypes, questionId: String) {
        openFeedbackFragment(feedbackTypes, questionId)
    }
}
