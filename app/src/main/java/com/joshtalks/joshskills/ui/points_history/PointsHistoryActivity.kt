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
import com.joshtalks.joshskills.databinding.ActivityPointsHistoryBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryDescViewHolder
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.*
import java.text.DecimalFormat

const val MENTOR_ID = "mentor_id"

class PointsHistoryActivity : WebRtcMiddlewareActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityPointsHistoryBinding
    private var mentorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(MENTOR_ID)) {
            mentorId = intent.getStringExtra(MENTOR_ID)
        }
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_points_history)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        initToolbar()
        viewModel.getPointsSummary(mentorId)
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
        text_message_title.text = getString(R.string.points_history)
    }

    private fun addObserver() {
        viewModel.pointsHistoryLiveData.observe(
            this,
            Observer {
                binding.userScore.text = DecimalFormat("#,##,##,###").format(it.totalPoints)
                binding.userScoreText.text = it.totalPointsText

                it.pointsHistoryDateList?.forEachIndexed { index, list ->
                    if (list.pointsSum != null) {
                        binding.recyclerView.addView(
                            PointsSummaryTitleViewHolder(
                                list.date!!,
                                list.pointsSum,
                                list.awardIconList,
                                index
                            )
                        )
                        list.pointsHistoryList?.forEachIndexed { index, pointsHistory ->
                            binding.recyclerView.addView(
                                PointsSummaryDescViewHolder(
                                    pointsHistory,
                                    index,
                                    list.pointsHistoryList.size
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    fun openPointsInfoTable() {
        startActivity(
            Intent(this, PointsInfoActivity::class.java).apply {
                putExtra(CONVERSATION_ID, intent.getStringExtra(CONVERSATION_ID))
            }
        )
    }

    companion object {
        fun startPointHistory(
            context: Activity,
            mentorId: String? = null,
            conversationId: String? = null
        ) {
            val intent = Intent(context, PointsHistoryActivity::class.java)
            intent.putExtra(CONVERSATION_ID, conversationId)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            mentorId?.run {
                intent.putExtra(MENTOR_ID, mentorId.toString())
            }
            context.startActivity(intent)
        }
    }
}
