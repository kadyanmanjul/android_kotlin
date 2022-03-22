package com.joshtalks.joshskills.ui.inbox

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.databinding.ActivityExtendFreeTrialBinding
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.activity_feed.viewModel.ActivityFeedViewModel
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import kotlinx.coroutines.flow.collect

const val INBOX_ENTITY = "inbox_entity"
class ExtendFreeTrialActivity : AppCompatActivity() {
    private lateinit var inboxEntity: InboxEntity
    private lateinit var inboxEntityExtended: InboxEntity
    lateinit var binding: ActivityExtendFreeTrialBinding
    private var extendFreeTrialBtnClicked = false

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            ExtendFreeTrialViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_extend_free_trial)
        binding.lifecycleOwner = this
        binding.handler = this
        initIntentObject()
        addObserver()
        binding.extendFreeTrialBtn.setOnClickListener {
            extendFreeTrialBtnClicked = true
            viewModel.extendFreeTrial()
        }
        binding.text1.setColorize()
    }

    private fun addObserver() {
        lifecycleScope.launchWhenStarted {
            viewModel.extendedFreeTrialCourseNetworkData.collect {
                inboxEntityExtended = it[0]
                if(extendFreeTrialBtnClicked){
                    ConversationActivity.startConversionActivity(this@ExtendFreeTrialActivity, inboxEntityExtended)
                    this@ExtendFreeTrialActivity.finish()
                }
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

    fun openConversationActivity(){
        ConversationActivity.startConversionActivity(this, inboxEntity)
        this.finish()
    }

        fun TextView.setColorize() {
            val spannable: Spannable = SpannableString(text)
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#107BE5")),
                7,
                31,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                7,
                31,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                AlphaAnimation(0.7f, 1f),
                7,
                31,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setText(spannable, TextView.BufferType.SPANNABLE)
        }

        companion object {
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
