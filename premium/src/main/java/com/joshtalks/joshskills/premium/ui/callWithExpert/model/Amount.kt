package com.joshtalks.joshskills.premium.ui.callWithExpert.model

import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.toRupees

data class Amount(
    val amount: Int,
    var id: Int
) {
    fun amountInRupees() = amount.toRupees()
}