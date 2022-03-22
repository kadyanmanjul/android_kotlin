package com.joshtalks.joshskills.ui.activity_feed

import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityFeedMainBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.activity_feed.utils.FEED_SCROLL_TO_END
import com.joshtalks.joshskills.ui.activity_feed.utils.ON_FEED_BACK_PRESSED
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_FEED_USER_PROFILE
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_PROFILE_IMAGE_FRAGMENT
import com.joshtalks.joshskills.ui.activity_feed.viewModel.ActivityFeedViewModel
import com.joshtalks.joshskills.ui.group.BaseGroupActivity
import com.joshtalks.joshskills.ui.userprofile.ProfileImageShowFragment
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class ActivityFeedMainActivity : BaseGroupActivity() {
    lateinit var layoutManager: LinearLayoutManager

    val binding by lazy<ActivityFeedMainBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_feed_main)
    }

    val viewModel: ActivityFeedViewModel by lazy {
        ViewModelProvider(this).get(ActivityFeedViewModel::class.java)
    }
    lateinit var recyclerView: RecyclerView

    override fun setIntentExtras() {}

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        getData()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                ON_FEED_BACK_PRESSED -> popBackStack()
                OPEN_FEED_USER_PROFILE -> openUserProfileActivity(it.obj as ActivityFeedResponseFirebase)
                OPEN_PROFILE_IMAGE_FRAGMENT -> openProfileImageFragment(it.obj as ActivityFeedResponseFirebase)
                FEED_SCROLL_TO_END -> scrollToEnd()
            }
        }
    }

    private fun getData() {
        viewModel.getFeed()
        viewModel.getActivityFeed()
    }

    private fun popBackStack() {
        onBackPressed()
    }

    private fun openUserProfileActivity(activityFeedResponseFirebase: ActivityFeedResponseFirebase) {
        UserProfileActivity.startUserProfileActivity(
            this,
            activityFeedResponseFirebase.mentorId ?: EMPTY,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            "RECENT_CALL",
            conversationId = null
        )
    }

    private fun openProfileImageFragment(activityFeedResponseFirebase: ActivityFeedResponseFirebase) {
        ProfileImageShowFragment.newInstance(
            activityFeedResponseFirebase.mediaUrl,
            null,
            null,
            activityFeedResponseFirebase.mentorId ?: EMPTY,
            false
        )
            .show((this).supportFragmentManager.beginTransaction(), "ImageShow")
    }

    fun scrollToEnd() {
        recyclerView.layoutManager?.scrollToPosition(0)
        viewModel.isScrollToEndButtonVisible.set(false)
    }
}