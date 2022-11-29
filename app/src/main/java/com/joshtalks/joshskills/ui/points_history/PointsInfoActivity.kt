package com.joshtalks.joshskills.ui.points_history

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.ActivityPointsInfoBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsInfoViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel

class PointsInfoActivity : CoreJoshActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityPointsInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_points_info)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        initToolbar()
        viewModel.getPointsInfo()
    }
    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initToolbar() {
        with(findViewById<AppCompatImageView>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
                onBackPressed()
            }
        }
        with(findViewById<AppCompatImageView>(R.id.iv_help)) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                openHelpActivity()
            }
        }
        findViewById<AppCompatTextView>(R.id.text_message_title).text = getString(R.string.how_points_work_title)
    }

    private fun addObserver() {

        viewModel.pointsInfoLiveData.observe(
            this,
            Observer {
                binding.infoText.text = it.info
                it.pointsWorkingList?.forEachIndexed() { index, pointsWorking ->
                    binding.recyclerView.addView(PointsInfoViewHolder(pointsWorking, index))
                }
            }
        )
    }
}
