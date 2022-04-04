package com.joshtalks.joshskills.ui.inbox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.ActivityExtendFreeTrialBinding
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.chat.ConversationActivity

class ExtendFreeTrialActivity : AppCompatActivity() {
    private lateinit var inboxEntity: InboxEntity
    lateinit var binding: ActivityExtendFreeTrialBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(
            ExtendFreeTrialViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_extend_free_trial)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        initIntentObject()
        addObserver()
        viewModel.postGoal(GoalKeys.EFT_SCREEN_SEEN.name, CampaignKeys.EXTEND_FREE_TRIAL.name)
    }

    private fun addObserver() {
        viewModel.singleLiveEvent.observe(this){
            when(it.what){
                OPEN_CONVERSATION_ACTIVITY->openConversationActivity(inboxEntity)
                OPEN_EFT_CONVERSATION_ACTIVITY->openConversationActivity(it.obj as InboxEntity)
            }
        }
    }

    private fun initIntentObject() {
        if (intent.hasExtra(INBOX_ENTITY)) {
            val temp = intent.getParcelableExtra(INBOX_ENTITY) as InboxEntity?
            if (temp == null) {
                this.finish()
                return
            }
            inboxEntity = temp
        }
    }

    fun openConversationActivity(inboxEntity: InboxEntity){
        ConversationActivity.startConversionActivity(this, inboxEntity)
        this.finish()
    }
    
    companion object {
            const val INBOX_ENTITY = "inbox_entity"

            fun startExtendFreeTrialActivity(activity: Activity, inboxEntity: InboxEntity) {
            val intent = Intent(activity, ExtendFreeTrialActivity::class.java).apply {
                putExtra(INBOX_ENTITY, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_top)
        }
    }
}
