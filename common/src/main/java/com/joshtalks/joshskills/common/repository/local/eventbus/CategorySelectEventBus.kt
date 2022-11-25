package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.server.FAQCategory

data class CategorySelectEventBus(
    val categoryList: List<FAQCategory>,
    val selectedCategory: FAQCategory
)