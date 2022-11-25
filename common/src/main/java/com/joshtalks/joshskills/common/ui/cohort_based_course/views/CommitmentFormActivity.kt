package com.joshtalks.joshskills.common.ui.cohort_based_course.views

import android.icu.number.NumberFormatter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.BaseActivity
import com.joshtalks.joshskills.common.constants.CLOSE_ACTIVITY
import com.joshtalks.joshskills.common.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.common.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.common.constants.START_CONVERSATION_ACTIVITY
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.databinding.ActivityCommitmentFormBinding
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.ui.chat.ConversationActivity
import com.joshtalks.joshskills.common.ui.cohort_based_course.viewmodels.CommitmentFormViewModel
import com.joshtalks.joshskills.common.ui.leaderboard.constants.HAS_COMMITMENT_FORM_SUBMITTED
import com.joshtalks.joshskills.common.util.ReminderUtil
import kotlin.math.nextUp


class CommitmentFormActivity : com.joshtalks.joshskills.common.base.BaseActivity() {

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
                com.joshtalks.joshskills.common.constants.OPEN_PROMISE_FRAGMENT -> replaceWithPromiseFragment()
                com.joshtalks.joshskills.common.constants.OPEN_SCHEDULE_FRAGMENT -> replaceWithScheduleFragment()
                com.joshtalks.joshskills.common.constants.START_CONVERSATION_ACTIVITY -> {
                    PrefManager.put(HAS_COMMITMENT_FORM_SUBMITTED, true)
                    ConversationActivity.startConversionActivity(
                    this,
                    intent.extras?.get("inboxEntity") as InboxEntity
                ).also { finish() }
                }
                com.joshtalks.joshskills.common.constants.CLOSE_ACTIVITY -> finish()
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