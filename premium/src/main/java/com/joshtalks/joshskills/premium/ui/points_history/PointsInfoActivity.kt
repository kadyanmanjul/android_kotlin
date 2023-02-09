package com.joshtalks.joshskills.premium.ui.points_history

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.CoreJoshActivity
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.databinding.ActivityPointsInfoBinding
import com.joshtalks.joshskills.premium.track.CONVERSATION_ID
import com.joshtalks.joshskills.premium.ui.points_history.viewholder.PointsInfoViewHolder
import com.joshtalks.joshskills.premium.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.*

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

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
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
        text_message_title.text = getString(R.string.how_points_work_title)
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
