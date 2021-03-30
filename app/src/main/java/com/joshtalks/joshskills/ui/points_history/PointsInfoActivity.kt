package com.joshtalks.joshskills.ui.points_history

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.databinding.ActivityPointsInfoBinding
import com.joshtalks.joshskills.ui.points_history.viewholder.PointsInfoViewHolder
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.*

class PointsInfoActivity : WebRtcMiddlewareActivity() {
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
        text_message_title.text = getString(R.string.how_points_work_title)
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