package com.joshtalks.badebhaiya.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils

class ProfileActivity: AppCompatActivity() {

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
                handleSpeakerProfile(it.isSpeaker, it)
                if (it.profilePicUrl.isNullOrEmpty().not()) Utils.setImage(ivProfilePic, it.profilePicUrl.toString())
                tvUserName.text = getString(R.string.full_name_concatenated, it.firstName, it.lastName ?: EMPTY)
            }
        }
    }

    private fun handleSpeakerProfile(isSpeaker: Boolean, profileResponse: ProfileResponse) {
        binding.apply {
            if (isSpeaker) {
                tvProfileBio.text = profileResponse.bioText
                tvFollowers.text =
                    getString(R.string.bb_followers, profileResponse.followersCount.toString())
                btnFollow.text = getString(R.string.following)
                btnFollow.setBackgroundColor(resources.getColor(R.color.follow_button_stroke))
            } else {
                tvFollowers.text = getString(R.string.bb_following, profileResponse.followersCount.toString())
            }
        }
    }

    fun updateFollowStatus() {
        viewModel.updateFollowStatus()
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
}