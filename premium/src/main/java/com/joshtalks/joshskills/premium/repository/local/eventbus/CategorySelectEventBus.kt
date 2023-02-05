package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.server.FAQCategory

data class CategorySelectEventBus(
    val categoryList: List<FAQCategory>,
    val selectedCategory: FAQCategory
)