package com.joshtalks.joshskills.common.ui.inbox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.abTest.CampaignKeys
import com.joshtalks.joshskills.common.core.abTest.GoalKeys
import com.joshtalks.joshskills.common.core.abTest.VariantKeys
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.databinding.ActivityExtendFreeTrialBinding
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.ui.chat.ConversationActivity

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
        MixPanelTracker.publishEvent(MixPanelEvent.EXTEND_FREE_TRIAL_OPENED)
            .addParam(ParamKeys.VARIANT, VariantKeys.EFT_ENABLED.name)
            .addParam(ParamKeys.CAMPAIGN, CampaignKeys.EXTEND_FREE_TRIAL.name)
            .push()
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
