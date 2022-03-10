package com.joshtalks.joshskills.ui.activity_feed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityFeedMainBinding
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ProfilePicture
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.activity_feed.viewModel.ActivityFeedViewModel
import com.joshtalks.joshskills.ui.userprofile.PreviousPicsAdapter
import com.joshtalks.joshskills.ui.userprofile.ProfileImageShowFragment

class ActivityFeedMainActivity : BaseActivity() {
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
    var adapter = ActivityFeedListAdapter(feedList,this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed_main)
        binding.lifecycleOwner = this
        startTime = System.currentTimeMillis()
        addObserver()
        getData()
        initView()
        addListeners()
    }

    private fun initView() {
        recyclerView = binding.rvFeeds
        layoutManager=LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
    }
    private fun getData() {
        viewModel.getFeed()
        viewModel.getActivityFeed()
    }

    override fun onBackPressed() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId.isNotBlank()) {
            viewModel.engageActivityFeedTime(impressionId, startTime)
        }
        super.onBackPressed()
    }
    private fun addObserver() {
        viewModel.apiCallStatus.observe(this) {
            when(it){
                ApiCallStatus.SUCCESS->
                    FullScreenProgressDialog.hideProgressBar(this)
                ApiCallStatus.FAILED-> {
                    FullScreenProgressDialog.hideProgressBar(this)
                    this.finish()
                }
                ApiCallStatus.START->
                    FullScreenProgressDialog.showProgressBar(this)
            }
        }
        viewModel.currentFeed.observe(this) {
                feedList.add(0,it)
                adapter.notifyItemInserted(0)
            if(layoutManager.findFirstCompletelyVisibleItemPosition()==0){
                binding.scrollToEndButton.visibility = View.GONE
                recyclerView.layoutManager?.scrollToPosition(0)
            }else{
                binding.scrollToEndButton.visibility = View.VISIBLE
            }

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
            recyclerView.layoutManager?.scrollToPosition(0)
            binding.scrollToEndButton.visibility = View.GONE

        }
    }
    companion object {
        fun startActivityFeedMainActivity(inboxEntity: InboxEntity, activity:Activity) {

            if (inboxEntity.isCourseBought.not() &&
                inboxEntity.expiryDate != null &&
                inboxEntity.expiryDate!!.time < System.currentTimeMillis()
            ) {
                val nameArr = User.getInstance().firstName?.split(SINGLE_SPACE)
                val firstName = if (nameArr != null) nameArr[0] else EMPTY
                showToast(activity.getString(R.string.feature_locked, firstName))
            } else {
                val intent = Intent(activity, ActivityFeedMainActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}