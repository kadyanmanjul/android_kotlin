package com.joshtalks.badebhaiya.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        viewModel.getProfileForUser(User.getInstance().userId)
        addObserver()
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
        if (isSpeaker) {
            binding.apply {
                tvProfileBio.text = profileResponse.bioText
                btnFollow.text = getString(R.string.following)
                btnFollow.setBackgroundColor(resources.getColor(R.color.follow_button_stroke))
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