package com.joshtalks.joshskills.core

import android.app.Activity
import android.content.Intent
import android.view.View
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_WITHOUT_CODE


abstract class CoreJoshActivity : BaseActivity() {


    override fun onStart() {
        super.onStart()
        try {
            findViewById<View>(R.id.iv_help).setOnClickListener {
                val i = Intent(this, HelpActivity::class.java)
                startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
            }
        } catch (ex: NullPointerException) {
        } catch (ex: Exception) {
        }
    }


    protected fun setResult() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    protected fun openCourseExplorerScreen() {
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_WITHOUT_CODE,
            null, true
        )
        this.finish()
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



