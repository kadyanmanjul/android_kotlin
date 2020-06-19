package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.FAQCategory

data class CategorySelectEventBus(
    val categoryList: List<FAQCategory>,
    val selectedCategory: FAQCategory
)