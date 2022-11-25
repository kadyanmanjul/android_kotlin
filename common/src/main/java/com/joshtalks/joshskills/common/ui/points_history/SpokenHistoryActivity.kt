package com.joshtalks.joshskills.common.ui.points_history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.CoreJoshActivity
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.databinding.ActivitySpokenHistoryBinding
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.common.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.common.ui.points_history.viewholder.SpokenSummaryDescViewHolder
import com.joshtalks.joshskills.common.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class SpokenHistoryActivity : CoreJoshActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivitySpokenHistoryBinding
    private var mentorId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(MENTOR_ID)) {
            mentorId = intent.getStringExtra(MENTOR_ID)
        }
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_spoken_history)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        initToolbar()
        viewModel.getSpokenMinutesSummary(mentorId)
        showProgressBar()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID)
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
                onBackPressed()
            }
        }
        with(iv_help) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                openHelpActivity()
            }
        }
        text_message_title.text = getString(R.string.minutes_history)
    }

    private fun addObserver() {
        viewModel.spokenHistoryLiveData.observe(
            this,
            Observer {
                binding.userScore.text = DecimalFormat("#,##,##,###").format(it.totalMinutesSpoken)
                binding.userScoreText.text = it.totalMinutesSpokenText
                scope.launch {
                    it.spokenHistoryDateList?.forEachIndexed { index, list ->
                        if (list.SpokenSum != null) {
                            withContext(Dispatchers.Main) {
                                binding.recyclerView.addView(
                                    PointsSummaryTitleViewHolder(
                                        list.date!!,
                                        list.SpokenSum.toInt(),
                                        arrayListOf(),
                                        index
                                    )
                                )
                            }
                            list.spokenHistoryList?.forEachIndexed { index, spokenHistory ->
                                withContext(Dispatchers.Main) {
                                    binding.recyclerView.addView(
                                        SpokenSummaryDescViewHolder(
                                            spokenHistory,
                                            index,
                                            list.spokenHistoryList.size
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        viewModel.apiCallStatusLiveData.observe(
            this,
            Observer {
                hideProgressBar()
            }
        )
    }

    companion object {

        const val MENTOR_ID = "mentor_id"

        fun startSpokenMinutesHistory(
            context: Activity,
            mentorId: String? = null,
            conversationId: String? = null
        ) {
            val intent = Intent(context, SpokenHistoryActivity::class.java)
            intent.putExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID, conversationId)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            mentorId?.run {
                intent.putExtra(MENTOR_ID, mentorId.toString())
            }
            context.startActivity(intent)
        }
    }
}
