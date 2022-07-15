package com.joshtalks.joshskills.ui.cohort_based_course.views

import android.icu.number.NumberFormatter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.CLOSE_ACTIVITY
import com.joshtalks.joshskills.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.constants.START_CONVERSATION_ACTIVITY
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivityCommitmentFormBinding
import com.joshtalks.joshskills.base.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.cohort_based_course.viewmodels.CommitmentFormViewModel
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_COMMITMENT_FORM_SUBMITTED
import com.joshtalks.joshskills.util.ReminderUtil
import kotlin.math.nextUp


class CommitmentFormActivity : BaseActivity() {

    private val binding by lazy<ActivityCommitmentFormBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_commitment_form)
    }

    val vm by lazy {
        ViewModelProvider(this)[CommitmentFormViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun onCreated() {
        addCommitmentFormLaunchFragment()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                OPEN_PROMISE_FRAGMENT -> replaceWithPromiseFragment()
                OPEN_SCHEDULE_FRAGMENT -> replaceWithScheduleFragment()
                START_CONVERSATION_ACTIVITY -> {
                    PrefManager.put(HAS_COMMITMENT_FORM_SUBMITTED, true)
                    ConversationActivity.startConversionActivity(
                    this,
                    intent.extras?.get("inboxEntity") as InboxEntity
                ).also { finish() }
                }
                CLOSE_ACTIVITY -> finish()
            }
        }
    }

    /*private fun onAlarmSetSuccess(reminderId: Int, startHour: Int, startMinute: Int) {
        ReminderUtil(applicationContext).apply {
            setAlarm(
                ReminderUtil.Companion.ReminderFrequency.EVERYDAY,
                getAlarmPendingIntent(reminderId),
                startHour,
                startMinute
            )
        }
        ConversationActivity.startConversionActivity(
            this,
            intent.extras?.get("inboxEntity") as InboxEntity
        ).also { finish() }
    }*/

    private fun replaceWithScheduleFragment() {
        supportFragmentManager.commit {
            replace(R.id.commitment_form_container, ScheduleFragment(), "ScheduleFragment")
            addToBackStack("ScheduleFragment")
        }
    }

    private fun replaceWithPromiseFragment() {
        supportFragmentManager.commit {
            replace(R.id.commitment_form_container, PromiseFragment(), "PromiseFragment")
            addToBackStack("PromiseFragment")
        }
    }

    private fun addCommitmentFormLaunchFragment() {
        supportFragmentManager.commit {
            add(
                R.id.commitment_form_container,
                CommitmentFormLaunchFragment(),
                "CommitmentFormLaunchFragment"
            )
        }
    }
}