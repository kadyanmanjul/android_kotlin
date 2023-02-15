package com.joshtalks.joshskills

data class SliderImageList(
    var images: List<SliderImage> = arrayListOf())

data class SliderImage(
    val imageUrl: String
)