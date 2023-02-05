package com.joshtalks.joshskills.premium.ui.payment

import com.joshtalks.joshskills.premium.core.AppObjectController

class PaymentRepository {

    private val network by lazy {
        AppObjectController.commonNetworkService
    }

    suspend fun verifyPayment(orderId: String) = network.verifyPaymentV3(orderId)
}