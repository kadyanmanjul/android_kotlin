package com.joshtalks.joshskills.ui.points_history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.databinding.ActivitySpokenHistoryBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.ui.points_history.viewholder.SpokenSummaryDescViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import java.text.DecimalFormat
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title

class SpokenHistoryActivity : WebRtcMiddlewareActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivitySpokenHistoryBinding
    private var mentorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_exit)
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
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(iv_help) {
            visibility = View.VISIBLE
            setOnClickListener {
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

                it.spokenHistoryDateList?.forEachIndexed { index, list ->
                    if (list.SpokenSum != null) {
                        binding.recyclerView.addView(
                            PointsSummaryTitleViewHolder(
                                list.date!!,
                                list.SpokenSum.toInt(),
                                arrayListOf(),
                                index
                            )
                        )
                        list.spokenHistoryList?.forEachIndexed { index, spokenHistory ->
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
            intent.putExtra(CONVERSATION_ID, conversationId)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            mentorId?.run {
                intent.putExtra(MENTOR_ID, mentorId.toString())
            }
            context.startActivity(intent)
        }
    }
}
