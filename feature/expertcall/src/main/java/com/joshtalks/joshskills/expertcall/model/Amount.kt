package com.joshtalks.joshskills.expertcall.model

import com.joshtalks.joshskills.expertcall.utils.toRupees

data class Amount(
    val amount: Int,
    var id: Int
) {
    fun amountInRupees() = amount.toRupees()
}