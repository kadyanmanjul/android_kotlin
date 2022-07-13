package com.joshtalks.badebhaiya.signup.response

import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.feed.model.Users

data class BBtoFollow(
    val speakers: List<Users>
)
data class FansList(
    val follower_data: List<Fans>
)