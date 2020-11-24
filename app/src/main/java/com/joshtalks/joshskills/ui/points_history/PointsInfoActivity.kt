package com.joshtalks.joshskills.ui.points_history

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityPointsHistoryBinding
import com.joshtalks.joshskills.databinding.ActivityPointsInfoBinding
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsInfoViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel

class PointsInfoActivity : BaseActivity() {
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
        viewModel.getPointsInfo()
    }

    private fun addObserver() {

        viewModel.pointsInfoLiveData.observe(this, Observer {
            binding.infoText.text=it.info
            it.pointsWorkingList?.forEachIndexed() { index, pointsWorking ->
                binding.recyclerView.addView(PointsInfoViewHolder(pointsWorking,index))
            }
        })
    }
}