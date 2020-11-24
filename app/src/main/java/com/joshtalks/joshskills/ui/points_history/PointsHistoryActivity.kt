package com.joshtalks.joshskills.ui.points_history

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityPointsHistoryBinding
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryDescViewHolder
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import com.mindorks.placeholderview.ExpandablePlaceHolderView


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
        viewModel.getPointsSummary()
    }

    private fun addObserver() {
        viewModel.pointsHistoryLiveData.observe(this, Observer {
            binding.userScore.text = it.totalPoints.toString()
            binding.userScoreText.text = it.totalPointsText

            it.pointsHistoryDateList?.forEach {
                binding.recyclerView.addView(PointsSummaryTitleViewHolder(it.date!!,it.pointsSum!!))
                it.pointsHistoryList?.forEach {
                    binding.recyclerView.addView(PointsSummaryDescViewHolder(it))
                }
            }
        })

    }

    public fun openPointsInfoTable(){
        startActivity(Intent(this,PointsInfoActivity::class.java))
    }
}