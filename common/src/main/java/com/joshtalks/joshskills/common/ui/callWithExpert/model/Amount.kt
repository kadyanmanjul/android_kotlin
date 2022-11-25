package com.joshtalks.joshskills.common.ui.callWithExpert.model

import com.joshtalks.joshskills.common.ui.callWithExpert.utils.toRupees

data class Amount(
    val amount: Int,
    var id: Int
) {
    fun amountInRupees() = amount.toRupees()
}