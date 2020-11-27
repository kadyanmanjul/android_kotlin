package com.joshtalks.joshskills.ui.points_history

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityPointsHistoryBinding
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryDescViewHolder
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title


class PointsHistoryActivity : BaseActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityPointsHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_points_history)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        initToolbar()
        viewModel.getPointsSummary()
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
        viewModel.pointsHistoryLiveData.observe(this, Observer {
            binding.userScore.text = it.totalPoints.toString()
            binding.userScoreText.text = it.totalPointsText

            it.pointsHistoryDateList?.forEachIndexed {index,list->
                binding.recyclerView.addView(PointsSummaryTitleViewHolder(list.date!!,list.pointsSum!!,index))
                list.pointsHistoryList?.forEachIndexed { index, pointsHistory ->
                    binding.recyclerView.addView(PointsSummaryDescViewHolder(pointsHistory,index,list.pointsHistoryList.size))
                }
            }
        })

    }

    public fun openPointsInfoTable(){
        startActivity(Intent(this,PointsInfoActivity::class.java))
    }
}