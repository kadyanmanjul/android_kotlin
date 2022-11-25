package com.joshtalks.joshskills.common.repository.local.eventbus

data class LandingPageCategorySelectEventBus(
    val position: Int,
    val categoryId :Int,
    val selectedCategory: String
)