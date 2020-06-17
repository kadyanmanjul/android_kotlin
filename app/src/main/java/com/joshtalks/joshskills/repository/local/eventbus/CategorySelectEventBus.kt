package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.TypeOfHelpModel

data class CategorySelectEventBus(
    val categoryList: List<TypeOfHelpModel>,
    val selectedCategory: TypeOfHelpModel
)