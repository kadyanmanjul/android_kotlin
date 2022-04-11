package com.joshtalks.joshskills.ui.activity_feed

import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityFeedMainBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponse
import com.joshtalks.joshskills.ui.activity_feed.utils.*
import com.joshtalks.joshskills.ui.activity_feed.viewModel.ActivityFeedViewModel
import com.joshtalks.joshskills.ui.group.BaseGroupActivity
import com.joshtalks.joshskills.ui.userprofile.ProfileImageShowFragment
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class ActivityFeedMainActivity : BaseGroupActivity() {

    val binding by lazy<ActivityFeedMainBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_feed_main)
    }

    val viewModel: ActivityFeedViewModel by lazy {
        ViewModelProvider(this).get(ActivityFeedViewModel::class.java)
    }
    private var flag:Boolean=false
    override fun setIntentExtras() {}

    override fun initViewBinding() {
        binding.vm = viewModel
        lifecycle.addObserver(viewModel)
        binding.executePendingBindings()
    }

    override fun onCreated() {
        getData()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                ON_FEED_BACK_PRESSED -> popBackStack()
                OPEN_FEED_USER_PROFILE -> openUserProfileActivity(it.obj as ActivityFeedResponse)
                OPEN_PROFILE_IMAGE_FRAGMENT -> openProfileImageFragment(it.obj as ActivityFeedResponse)
                FEED_SCROLL_TO_END -> scrollToEnd()
                ON_ITEM_ADDED->setScrollToEndBtn()
            }
        }
    }

    private fun getData() {
        viewModel.getActivityFeed("")
    }

    private fun popBackStack() {
        onBackPressed()
    }

    private fun openUserProfileActivity(activityFeedResponse: ActivityFeedResponse) {
        UserProfileActivity.startUserProfileActivity(
            this,
            activityFeedResponse.mentorId ?: EMPTY,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            "RECENT_CALL",
            conversationId = null
        )
    }

    private fun openProfileImageFragment(activityFeedResponse: ActivityFeedResponse) {
        ProfileImageShowFragment.newInstance(
            activityFeedResponse.photoUrl,
            null,
            null,
            activityFeedResponse.mentorId ?: EMPTY,
            false
        )
            .show((this).supportFragmentManager.beginTransaction(), "ImageShow")
    }
    fun setScrollToEndBtn(){
        val layout=binding.rvFeeds.layoutManager as LinearLayoutManager
        if(layout.findFirstCompletelyVisibleItemPosition()==0){
            viewModel.isScrollToEndButtonVisible.set(false)
            binding.rvFeeds.layoutManager?.scrollToPosition(0)
        }else{
            if(flag) {
                viewModel.isScrollToEndButtonVisible.set(true)
                flag=true
            }
        }
    }
    fun scrollToEnd() {
        binding.rvFeeds.layoutManager?.scrollToPosition(0)
        viewModel.isScrollToEndButtonVisible.set(false)
    }
}