package com.joshtalks.badebhaiya.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils

class ProfileActivity: AppCompatActivity(), FeedAdapter.ConversationRoomItemCallback {

    private val binding by lazy<ActivityProfileBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_profile)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private var userId: String? = EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        handleIntent()
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId))
        addObserver()
        setOnClickListener()
    }

    private fun setOnClickListener() {
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            super.onBackPressed()
        }
    }

    private fun handleIntent() {
        userId = intent.getStringExtra(USER_ID)
        if (userId.isNullOrEmpty()) User.getInstance().userId
    }

    private fun addObserver() {
        viewModel.userProfileData.observe(this) {
            binding.apply {
                handleSpeakerProfile(it)
                if (it.profilePicUrl.isNullOrEmpty().not()) Utils.setImage(ivProfilePic, it.profilePicUrl.toString())
                else
                    Utils.setImage(ivProfilePic,it.firstName.toString())
                tvUserName.text = getString(R.string.full_name_concatenated, it.firstName, it.lastName)
            }
        }
        viewModel.speakerFollowed.observe(this) {
            if (it == true) {
                speakerFollowedUIChanges()
            }
        }
    }

    private fun handleSpeakerProfile(profileResponse: ProfileResponse) {
        binding.apply {
            if (profileResponse.isSpeaker) {
                tvProfileBio.text = profileResponse.bioText
                tvFollowers.text = HtmlCompat.fromHtml(getString(R.string.bb_followers, profileResponse.followersCount.toString()),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (profileResponse.isSpeakerFollowed) {
                    speakerFollowedUIChanges()
                }
            } else {
                tvFollowers.text = getString(R.string.bb_following, profileResponse.followersCount.toString())
            }
        }
    }

    fun updateFollowStatus() {
        viewModel.updateFollowStatus()
    }

    private fun speakerFollowedUIChanges() {
        binding.apply {
            btnFollow.text = getString(R.string.following)
            btnFollow.setTextColor(resources.getColor(R.color.white))
            btnFollow.background = AppCompatResources.getDrawable(this@ProfileActivity,
                R.drawable.following_button_background)
        }
    }

    companion object {
        fun openProfileActivity(context: Context, userId: String = EMPTY) {
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(USER_ID, userId)
            }.run {
                context.startActivity(this)
            }
        }
        fun getIntent(context: Context, userId: String = EMPTY): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(USER_ID, userId)
            }
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {

    }

    override fun setReminder(room: RoomListResponseItem, view: View) {

    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {

    }
}