package com.joshtalks.joshskills.ui.activity_feed

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityFeedMainBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.activity_feed.viewModel.ActivityFeedViewModel
import java.util.*

class ActivityFeedMainActivity : AppCompatActivity() {
    lateinit var binding: ActivityFeedMainBinding
    private var startTime = 0L
    private var impressionId: String = EMPTY
    lateinit var layoutManager:LinearLayoutManager

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            ActivityFeedViewModel::class.java
        )
    }
    lateinit var recyclerView: RecyclerView
    var feedList = ArrayList<ActivityFeedResponseFirebase>()
    var adapter = ActivityFeedListAdapter(feedList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed_main)
        binding.lifecycleOwner = this
        startTime = System.currentTimeMillis()
        initView()
        getData()
        addObserver()
        addListeners()
    }

    private fun initView() {
        recyclerView = binding.rvFeeds
        layoutManager=LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager

        }

    }
    private fun getData() {
        viewModel.getActivityFeed()
        viewModel.getFeed()
    }

    override fun onBackPressed() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId.isBlank().not()) {
            viewModel.engageActivityFeedTime(impressionId, startTime)
        }
        super.onBackPressed()
    }
    private fun addObserver() {
        viewModel.apiCallStatus.observe(this) {
            if (it == ApiCallStatus.SUCCESS) {
                FullScreenProgressDialog.hideProgressBar(this)
            } else if (it == ApiCallStatus.FAILED) {
                FullScreenProgressDialog.hideProgressBar(this)
                this.finish()
            } else if (it == ApiCallStatus.START) {
                FullScreenProgressDialog.showProgressBar(this)
            }
        }
        viewModel.currentFeed.observe(this) {

            if(layoutManager.findFirstCompletelyVisibleItemPosition()==0){
                binding.scrollToEndButton.visibility = View.GONE
                recyclerView.post { recyclerView.smoothScrollToPosition(0) }
            }else{
                binding.scrollToEndButton.visibility = View.VISIBLE
            }
            feedList.add(0,it)
            adapter.notifyItemInserted(0)

//            recyclerView.post { recyclerView.smoothScrollToPosition(0) }
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    //some code when initially scrollState changes
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                }
            })

        }
        viewModel.feedDataList.observe(this) {
           it?.let {
               impressionId=it.impressionId!!
           }

       }
    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.scrollToEndButton.setOnClickListener{
            recyclerView.post{recyclerView.smoothScrollToPosition(0)}
            binding.scrollToEndButton.visibility = View.GONE

        }
    }
}