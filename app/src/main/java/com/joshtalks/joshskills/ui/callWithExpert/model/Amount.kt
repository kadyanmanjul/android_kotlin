package com.joshtalks.joshskills.ui.callWithExpert.model

import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees

data class Amount(
    val amount: Int,
    val id: Int
) {
    fun amountInRupees() = amount.toRupees()
}