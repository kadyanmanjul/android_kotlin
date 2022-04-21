package com.joshtalks.badebhaiya.feed.model

import com.joshtalks.badebhaiya.profile.request.FollowRequest

data class Users(
    val user_name: String,
    val bio: String,
    val profilePicUrl: String,
    val followRequest: FollowRequest,
)