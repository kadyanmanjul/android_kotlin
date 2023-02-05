package com.joshtalks.joshskills.premium.repository.local.eventbus

data class LandingPageCategorySelectEventBus(
    val position: Int,
    val categoryId :Int,
    val selectedCategory: String
)